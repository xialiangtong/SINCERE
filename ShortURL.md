Problem: Build a URL Shortener Service
You’re building a small service like bit.ly. It should let users create short links and resolve (redirect) them later.

Core requirements
Shorten a URL
Input: a long URL (string)
Output: a short code (string) and the short URL (e.g., https://sho.rt/Ab3k9Z)
Resolve a short code
Input: short code

Output: the original long URL (or an HTTP redirect to it)

Follow my instruction for the design/implementation.
Let me share my ideas about the solution :
1. need a top level class ShortURLService
2. need an inner class ShortURLResult, includes fields : string longURL, string code, string shortURL.
3. need a class to store the url data, let's use interface for extension : URLRepository, define method saveURL(String code, String longURL), String getLongURL(String code), String getCode(String longURL), boolean existCode(String code).
4. have a class InMemoryURLRepository to implement this interface, having fields : 
Map<String, String> codeToLongURL (map code  -> longURL), 
Map<String, String> longURLToCode (map longURL -> code)
5. need another interface CodeGenerator, define a method String generateCode(String longURL)
   (returns only the code string — building ShortURLResult is ShortURLService's job)
6. have a class Base62CodeGenerator to implement CodeGenerator interface, which will use base62 encoding to generate a code from longURL
7. in the top class, we have fields : 
final String prefix (shortURL = prefix+code), 
CodeGenerator codeGenerator, 
URLRepository repository.
we have methods : 
ShortURLResult shortenURL(String longURL) :
  - validate input (null, empty, invalid URL)
  - check duplicate: repository.getCode(longURL) → if exists, return same ShortURLResult
  - generate code via codeGenerator
  - collision handling: while (repository.existCode(code)) { regenerate }
  - store in repository
  - return ShortURLResult
String getLongURL(String code) -> read from repository


// In-memory store first; add collision handling; expiry; tests

// Talk about scaling briefly, but implement core correctness

java -cp target/classes ShortURLSolution