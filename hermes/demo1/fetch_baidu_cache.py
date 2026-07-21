import urllib.request, urllib.error, ssl, re, json

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

# Get the Baidu search page and extract the cache link
search_url = 'https://www.baidu.com/s?wd=zhuanlan.zhihu.com%2Fp%2F4130861066'
req = urllib.request.Request(search_url, data=None, headers={
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
    'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
    'Accept-Language': 'zh-CN,zh;q=0.9',
})
resp = urllib.request.urlopen(req, timeout=10, context=ctx)
text = resp.read().decode('utf-8', errors='replace')

# Find cache links
cache_links = re.findall(r'<a[^>]*href="(http[s]?://cache\.baiducontent\.com[^"]+)"', text)
print(f'Found {len(cache_links)} cache links')
for l in cache_links[:3]:
    print(f'  {l}')

# Also look for the actual result link with mu attribute
mu_values = re.findall(r'mu="(https?://[^"]+)"', text)
for m in mu_values[:5]:
    print(f'  mu: {m}')

# Try to find the result container with the zhihu link
# Look for the specific div that contains the result
sections = re.findall(r'<div[^>]*class="result[^"]*c-container[^"]*"[^>]*id="(\d+)"[^>]*>(.*?)</div>\s*</div>', text, re.DOTALL)
for sid, content in sections:
    if 'zhuanlan.zhihu.com' in content:
        # Extract the abstract
        abstract_m = re.search(r'<span[^>]*class="content-right_[^"]*"[^>]*>(.*?)</span>', content, re.DOTALL)
        if abstract_m:
            abstract = re.sub(r'<[^>]+>', '', abstract_m.group(1)).strip()
            print(f'\nAbstract ({sid}): {abstract[:500]}')
        
        # Also check for content abstract
        ca_m = re.search(r'<div[^>]*class="c-abstract"[^>]*>(.*?)</div>', content, re.DOTALL)
        if ca_m:
            ca = re.sub(r'<[^>]+>', '', ca_m.group(1)).strip()
            print(f'c-abstract: {ca[:500]}')
        break

# Try direct Baidu cache URL format
# Baidu cache format: https://cache.baiducontent.com/c?m=... 
# Let's try to construct it
# First, URL encode the target URL
import urllib.parse
target_url = 'https://zhuanlan.zhihu.com/p/4130861066'
encoded = urllib.parse.quote(target_url, safe='')
cache_url = f'https://cache.baiducontent.com/c?m={encoded}'
print(f'\nTrying cache URL: {cache_url}')

try:
    req2 = urllib.request.Request(cache_url, data=None, headers={
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
    })
    resp2 = urllib.request.urlopen(req2, timeout=10, context=ctx)
    cache_text = resp2.read().decode('utf-8', errors='replace')
    print(f'Cache: {resp2.status} {len(cache_text)} chars')
    
    # Extract content
    # Remove scripts and styles
    cleaned = re.sub(r'<script[^>]*>.*?</script>', '', cache_text, flags=re.DOTALL)
    cleaned = re.sub(r'<style[^>]*>.*?</style>', '', cleaned, flags=re.DOTALL)
    
    # Find the main content
    # Look for the cached content div
    m = re.search(r'<div[^>]*id="content"[^>]*>(.*?)</div>', cleaned, flags=re.DOTALL)
    if m:
        content = m.group(1)
        # Strip HTML
        text_content = re.sub(r'<[^>]+>', '\n', content)
        text_content = re.sub(r'\n{3,}', '\n\n', text_content).strip()
        print(f'Content: {text_content[:2000]}')
    else:
        # Try to find body content
        m2 = re.search(r'<body[^>]*>(.*?)</body>', cleaned, flags=re.DOTALL)
        if m2:
            body = m2.group(1)
            text_content = re.sub(r'<[^>]+>', '\n', body)
            text_content = re.sub(r'\n{3,}', '\n\n', text_content).strip()
            print(f'Body: {text_content[:2000]}')
        else:
            print('No body found')
            print(cleaned[:1000])
except Exception as e:
    print(f'Cache Error: {type(e).__name__}: {e}')
