Problem: Build a URL Shortener Service

You’re building a small service like bit.ly. It should let users create short links and resolve (redirect) them later.

Core requirements

Shorten a URL

Input: a long URL (string)

Output: a short code (string) and the short URL (e.g., https://sho.rt/Ab3k9Z)

Resolve a short code

Input: short code

Output: the original long URL (or an HTTP redirect to it)


In-memory store first; add collision handling; expiry; tests

Talk about scaling briefly, but implement core correctness