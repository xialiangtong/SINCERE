"""
FlightPrice — Direct Flight Price Scanner

Uses Playwright to query Google Flights per-day:
  1. Sets up a SEA→PVG (or any route) search with nonstop filter.
  2. For each of the next N days, navigates to that date via the date picker.
  3. Reads the lowest nonstop price from the results page.
  4. Prints Economy and Premium Economy sorted low→high.

Setup:
    cd FlightPrice
    ./setup.sh

Usage:
    python3 flight_price_scanner.py --from SEA --to PVG
    python3 flight_price_scanner.py --from SEA --to PVG --days 14
    python3 flight_price_scanner.py --from LAX --to NRT --no-headless
"""

from __future__ import annotations

import argparse
import asyncio
import re
from datetime import date, timedelta
from pathlib import Path

from playwright.async_api import async_playwright, TimeoutError as PWTimeoutError

# ─── Config ────────────────────────────────────────────────────────────────────
SCREENSHOT_DIR = Path(__file__).parent / "screenshots"
PAGE_TIMEOUT   = 30_000

MONTH_NAMES = [
    "January","February","March","April","May","June",
    "July","August","September","October","November","December",
]

CABINS = [
    ("Economy",         "Economy"),
    ("Premium economy", "Premium Economy"),
]


# ─── Helpers ───────────────────────────────────────────────────────────────────
def _parse_usd(text: str) -> int | None:
    m = re.search(r"\$\s*([\d,]+)", text)
    if m:
        try:
            return int(m.group(1).replace(",", ""))
        except ValueError:
            pass
    return None


async def _screenshot(page, name: str) -> None:
    SCREENSHOT_DIR.mkdir(parents=True, exist_ok=True)
    await page.screenshot(path=str(SCREENSHOT_DIR / f"{name}.png"), full_page=False)


async def _dismiss_dialogs(page) -> None:
    for label in ["Accept all", "Agree", "I agree", "Accept"]:
        try:
            await page.get_by_role("button", name=re.compile(label, re.I)).click(timeout=2_000)
            await asyncio.sleep(0.5)
            return
        except Exception:
            pass


# ─── Form helpers ──────────────────────────────────────────────────────────────
async def _set_cabin(page, cabin_label: str) -> None:
    if cabin_label == "Economy":
        return
    try:
        cabin_div = page.locator("div[role='combobox']").filter(
            has_text=re.compile(r"^economy$", re.I)
        ).first
        await cabin_div.click(timeout=5_000)
        await asyncio.sleep(0.5)
        await page.get_by_role("option", name=re.compile(cabin_label, re.I)).click(timeout=5_000)
        await asyncio.sleep(0.5)
    except Exception as e:
        print(f"    [cabin warning: {e}]")


async def _fill_airport(page, aria_pattern: str, iata: str) -> None:
    """Type IATA code into airport field and select the airport autocomplete option."""
    field = page.locator(f"input[role='combobox'][aria-label*='{aria_pattern}']").first
    await field.fill("")
    await field.click(timeout=5_000)
    await asyncio.sleep(0.3)
    await page.keyboard.type(iata, delay=100)
    await asyncio.sleep(2.5)
    airport_opt = page.get_by_role("option").filter(has_text=re.compile(r"airport", re.I))
    try:
        await airport_opt.first.click(timeout=6_000)
    except Exception:
        await page.get_by_role("option").first.click(timeout=3_000)
    await asyncio.sleep(0.5)


async def _fill_to_airport(page, iata: str) -> None:
    to_field = page.get_by_placeholder("Where to?")
    await to_field.click(timeout=5_000)
    await asyncio.sleep(0.3)
    await page.keyboard.type(iata, delay=100)
    await asyncio.sleep(2.5)
    airport_opt = page.get_by_role("option").filter(has_text=re.compile(r"airport", re.I))
    try:
        await airport_opt.first.click(timeout=6_000)
    except Exception:
        await page.get_by_role("option").first.click(timeout=3_000)
    await asyncio.sleep(0.5)


async def _apply_nonstop_filter(page) -> bool:
    """Apply the Nonstop filter. Returns True on success."""
    try:
        stops_btn = page.get_by_role("button", name=re.compile(r"stops?", re.I)).first
        await stops_btn.click(timeout=8_000)
        await asyncio.sleep(0.8)
        nonstop = page.get_by_role("radio", name=re.compile(r"nonstop", re.I))
        await nonstop.click(timeout=5_000)
        await asyncio.sleep(0.5)
        try:
            done = page.get_by_role("button", name=re.compile(r"^(done|close)$", re.I))
            await done.click(timeout=3_000)
        except Exception:
            await page.keyboard.press("Escape")
        await asyncio.sleep(2.0)
        return True
    except Exception as e:
        print(f"    [nonstop filter warning: {e}]")
        return False


# ─── Calendar date navigation ─────────────────────────────────────────────────
async def _current_calendar_months(page) -> list[tuple[int, int]]:
    """Return (year, month) pairs for all visible calendar month headers."""
    result = await page.evaluate("""() => {
        const names = ['January','February','March','April','May','June',
                       'July','August','September','October','November','December'];
        const pat = /\\b(January|February|March|April|May|June|July|August|September|October|November|December)\\s+(\\d{4})\\b/g;
        const found = [], seen = new Set();
        const walk = (node) => {
            if (node.nodeType === 3) {
                let m;
                while ((m = pat.exec(node.textContent)) !== null) {
                    const idx = names.findIndex(n => n === m[1]);
                    const key = m[2] + '-' + (idx + 1);
                    if (idx >= 0 && !seen.has(key)) { seen.add(key); found.push([+m[2], idx + 1]); }
                }
            } else { for (const c of node.childNodes) walk(c); }
        };
        walk(document.body);
        return found;
    }""")
    return [tuple(r) for r in (result or [])]


async def _navigate_calendar_to_month(page, target_year: int, target_month: int) -> None:
    """Click the next-month arrow until the calendar shows target month."""
    for _ in range(24):
        months = await _current_calendar_months(page)
        if not months:
            break
        # Stop if target month is visible (either first or second panel)
        if (target_year, target_month) in months:
            break
        # Stop if we've passed the target
        if min(months) > (target_year, target_month):
            break
        try:
            next_btn = page.get_by_role("button", name=re.compile(r"next", re.I)).last
            await next_btn.click(timeout=4_000)
            await asyncio.sleep(0.8)
        except Exception:
            break


async def _open_date_picker(page) -> bool:
    """Open the departure date picker. Returns True if successful."""
    for selector in [
        "input[aria-label='Departure'][placeholder='Departure']",
        "input[aria-label='Departure']",
        "[aria-label*='Departure date']",
    ]:
        try:
            await page.locator(selector).first.click(timeout=4_000)
            await asyncio.sleep(1.5)
            # Verify calendar opened: look for a visible day-number button
            found = await page.evaluate("""() => {
                for (const el of document.querySelectorAll('[role="button"], button')) {
                    const t = el.innerText.trim();
                    if (/^\\d{1,2}$/.test(t) && +t >= 1 && +t <= 31
                            && el.offsetParent !== null) return true;
                }
                return false;
            }""")
            if found:
                return True
        except Exception:
            pass
    return False


async def _select_date_in_picker(page, dept_date: date) -> bool:
    """
    Navigate to dept_date in the open date picker and click it.
    Returns True on success.
    """
    await _navigate_calendar_to_month(page, dept_date.year, dept_date.month)

    day_str, month_str, year_str = (
        str(dept_date.day), MONTH_NAMES[dept_date.month - 1], str(dept_date.year)
    )

    # Try Playwright aria-label patterns (Google uses "Friday, April 14, 2026")
    patterns = [
        rf"\w+,\s+{month_str}\s+{day_str},?\s+{year_str}",   # "Friday, April 14, 2026"
        rf"{month_str}\s+{day_str},?\s+{year_str}",            # "April 14, 2026"
        rf"{day_str}\s+{month_str}\s+{year_str}",
        rf"{month_str[:3]}\s+{day_str},?\s+{year_str}",
    ]
    for pat in patterns:
        try:
            btn = page.get_by_role("button", name=re.compile(pat, re.I))
            if await btn.count() > 0:
                await btn.first.click(timeout=4_000)
                await asyncio.sleep(0.4)
                return True
        except Exception:
            pass

    # JS fallback: find any visible button whose aria-label contains month, day, year
    clicked = await page.evaluate(f"""() => {{
        const m = "{month_str}", d = "{day_str}", y = "{year_str}";
        for (const el of document.querySelectorAll('[role="button"], button')) {{
            const lbl = (el.getAttribute('aria-label') || '').trim();
            if (lbl.includes(m) && lbl.includes(d) && lbl.includes(y)
                    && el.offsetParent !== null) {{
                el.click(); return lbl;
            }}
        }}
        return null;
    }}""")
    if clicked:
        await asyncio.sleep(0.4)
        return True

    return False


async def _close_date_picker(page) -> None:
    """Press Done or Escape to close the date picker."""
    try:
        done = page.get_by_role("button", name=re.compile(r"^done$", re.I))
        await done.click(timeout=3_000)
    except Exception:
        await page.keyboard.press("Escape")
    await asyncio.sleep(0.5)


# ─── Price extraction from results ────────────────────────────────────────────
async def _extract_lowest_price(page) -> int | None:
    """
    Extract the lowest nonstop price shown on the current results page.
    Returns None if no nonstop flights are found.
    """
    # Check for "no nonstop flights" message
    content = await page.content()
    if re.search(r"no nonstop flights", content, re.I):
        return None

    prices: list[int] = []

    # Strategy 1: aria-label on flight result elements
    for el in await page.query_selector_all("[aria-label*='$']"):
        label = await el.get_attribute("aria-label") or ""
        v = _parse_usd(label)
        if v and 200 <= v <= 50_000:
            prices.append(v)

    if prices:
        return min(prices)

    # Strategy 2: full page scan for dollar amounts in a reasonable range
    for m in re.finditer(r"\$([\d,]+)", content):
        try:
            v = int(m.group(1).replace(",", ""))
            if 200 <= v <= 50_000:
                prices.append(v)
        except ValueError:
            pass

    return min(prices) if prices else None


# ─── Per-date scan ────────────────────────────────────────────────────────────
async def scan_dates(page, dates: list[date]) -> dict[date, int]:
    """
    For each date: open the date picker, navigate to the date, read the nonstop price.
    """
    results: dict[date, int] = {}

    for dept_date in dates:
        date_str = dept_date.strftime("%Y-%m-%d")
        print(f"  {date_str}…  ", end="", flush=True)

        try:
            # Open date picker
            if not await _open_date_picker(page):
                print("picker failed")
                continue

            # Select the date
            if not await _select_date_in_picker(page, dept_date):
                await _close_date_picker(page)
                print("date click failed")
                continue

            # Close picker (Done button)
            await _close_date_picker(page)

            # Re-click Search if the button is still visible (some date changes need it)
            try:
                search_btn = page.get_by_role("button", name=re.compile(r"^search$", re.I))
                if await search_btn.is_visible():
                    await search_btn.click(timeout=3_000)
            except Exception:
                pass
            await asyncio.sleep(1.0)

            # Wait for results to load
            try:
                await page.wait_for_load_state("networkidle", timeout=10_000)
            except PWTimeoutError:
                pass
            await asyncio.sleep(1.5)

            # Re-apply nonstop filter if it was lost
            content_check = await page.content()
            if not re.search(r"nonstop", content_check, re.I):
                await _apply_nonstop_filter(page)
                try:
                    await page.wait_for_load_state("networkidle", timeout=8_000)
                except PWTimeoutError:
                    pass
                await asyncio.sleep(1.0)

            price = await _extract_lowest_price(page)
            if price is not None:
                results[dept_date] = price
                print(f"${price:,}")
            else:
                print("no nonstop")

        except Exception as exc:
            print(f"error ({exc})")
            await _screenshot(page, f"error_{date_str}")

    return results


# ─── Full setup ────────────────────────────────────────────────────────────────
async def setup_search(page, from_iata: str, to_iata: str, cabin_label: str,
                       first_date: date) -> None:
    """
    Navigate to Google Flights, fill the search form with the first date,
    submit, and apply the nonstop filter.
    """
    await page.goto("https://www.google.com/flights",
                    wait_until="domcontentloaded", timeout=PAGE_TIMEOUT)
    await asyncio.sleep(2)
    await _dismiss_dialogs(page)

    # One-way
    try:
        trip_div = page.locator("div[role='combobox']").filter(
            has_text=re.compile(r"round trip", re.I)
        ).first
        await trip_div.click(timeout=4_000)
        await page.get_by_role("option", name=re.compile(r"one.?way", re.I)).click(timeout=4_000)
        await asyncio.sleep(0.5)
    except Exception:
        pass

    await _set_cabin(page, cabin_label)
    await _fill_airport(page, "Where from", from_iata)
    await _fill_to_airport(page, to_iata)

    # Set a departure date so Search goes to actual results
    date_str = first_date.strftime("%-m/%-d/%Y")
    try:
        dep = page.locator("input[aria-label='Departure'][placeholder='Departure']").first
        await dep.click(timeout=5_000)
        await asyncio.sleep(0.5)
        await page.keyboard.type(date_str, delay=80)
        await asyncio.sleep(0.5)
        await page.keyboard.press("Enter")
        await asyncio.sleep(0.5)
        await page.keyboard.press("Escape")
    except Exception as e:
        print(f"    [date set warning: {e}]")

    # Search
    try:
        await page.get_by_role("button", name=re.compile(r"^search$", re.I)).click(timeout=5_000)
    except Exception:
        await page.keyboard.press("Enter")
    await asyncio.sleep(4)

    # Apply nonstop filter
    ok = await _apply_nonstop_filter(page)
    if not ok:
        print("    [warning: nonstop filter may not have been applied]")

    await _screenshot(page, "setup_done")


# ─── Per-cabin orchestration ───────────────────────────────────────────────────
async def scrape_cabin(
    browser,
    from_iata: str,
    to_iata: str,
    dates: list[date],
    cabin_gf_label: str,
    cabin_display: str,
) -> dict[date, int]:
    page = await browser.new_page()
    await page.set_extra_http_headers({
        "Accept-Language": "en-US,en;q=0.9",
        "User-Agent": (
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
            "AppleWebKit/537.36 (KHTML, like Gecko) "
            "Chrome/124.0.0.0 Safari/537.36"
        ),
    })
    results: dict[date, int] = {}
    try:
        print(f"  Setting up ({cabin_display})…")
        await setup_search(page, from_iata, to_iata, cabin_gf_label, dates[0])
        print(f"  Scanning {len(dates)} dates ({cabin_display})…")
        results = await scan_dates(page, dates)
    except Exception as exc:
        print(f"  [fatal error: {exc}]")
        await _screenshot(page, f"fatal_{cabin_display.lower().replace(' ','_')}")
    finally:
        await page.close()
    return results


# ─── Output ────────────────────────────────────────────────────────────────────
def print_ranked(prices: dict[date, int], label: str, from_iata: str, to_iata: str) -> None:
    header = f"  {label}  —  Direct {from_iata} → {to_iata}  (low → high)"
    bar = "=" * max(len(header), 54)
    print(f"\n{bar}")
    print(header)
    print(bar)
    if not prices:
        print("  No prices found.")
        return
    for rank, (d, price) in enumerate(sorted(prices.items(), key=lambda x: x[1]), 1):
        print(f"  {rank:2}.  {d.strftime('%Y-%m-%d')}  ${price:,}")


# ─── Entry point ───────────────────────────────────────────────────────────────
def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser(
        description="Scan direct flight prices via Google Flights (sorted low→high)."
    )
    p.add_argument("--from", dest="from_iata", required=True,
                   help="Departure airport IATA code (e.g. SEA)")
    p.add_argument("--to",   dest="to_iata",   required=True,
                   help="Arrival airport IATA code (e.g. PVG)")
    p.add_argument("--days", type=int, default=30,
                   help="Days to scan from tomorrow (default: 30)")
    p.add_argument("--no-headless", dest="headless", action="store_false", default=True,
                   help="Show the browser window")
    return p.parse_args()


async def run() -> None:
    args   = parse_args()
    from_iata = args.from_iata.upper()
    to_iata   = args.to_iata.upper()
    today  = date.today()
    dates  = [today + timedelta(days=i) for i in range(1, args.days + 1)]

    print(f"\n{'─'*54}")
    print(f"  Direct Flight Price Scanner")
    print(f"  Route : {from_iata} → {to_iata}")
    print(f"  Dates : {dates[0]}  to  {dates[-1]}  ({args.days} days)")
    print(f"  Mode  : {'headless' if args.headless else 'browser visible'}")
    print(f"{'─'*54}\n")

    all_prices: dict[str, dict[date, int]] = {}

    async with async_playwright() as pw:
        browser = await pw.chromium.launch(headless=args.headless)
        try:
            for cabin_gf_label, cabin_display in CABINS:
                print(f"── {cabin_display} {'─'*40}")
                all_prices[cabin_display] = await scrape_cabin(
                    browser, from_iata, to_iata, dates, cabin_gf_label, cabin_display,
                )
                print()
        finally:
            await browser.close()

    for _, cabin_display in CABINS:
        print_ranked(all_prices[cabin_display], cabin_display, from_iata, to_iata)
    print()


if __name__ == "__main__":
    asyncio.run(run())
