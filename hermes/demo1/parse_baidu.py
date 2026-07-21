import urllib.request, urllib.error, ssl, re, json

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

# Try Baidu cache
url = 'https://cache.baiducontent.com/c?m='
# Or use the search result to find cached version
# First get the search page to find the actual result link
search_url = 'https://www.baidu.com/s?wd=zhuanlan.zhihu.com%2Fp%2F4130861066'
req = urllib.request.Request(search_url, data=None, headers={
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
    'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
    'Accept-Language': 'zh-CN,zh;q=0.9',
})
resp = urllib.request.urlopen(req, timeout=10, context=ctx)
text = resp.read().decode('utf-8', errors='replace')

# Find the actual result link
# Baidu wraps results in <div class="result" ...>
# Look for the zhihu link
links = re.findall(r'<a[^>]*href="(https?://zhuanlan\.zhihu\.com[^"]+)"', text)
for link in links[:3]:
    print(f'Found link: {link}')

# Also look for cache links
cache_links = re.findall(r'<a[^>]*href="(http://cache\.baiducontent\.com[^"]+)"', text)
for link in cache_links[:3]:
    print(f'Cache link: {link}')

# Look for the result div containing zhihu
results = re.findall(r'<div[^>]*class="result[^"]*"[^>]*>(.*?)</div>\s*</div>', text, re.DOTALL)
for r in results:
    if 'zhuanlan.zhihu.com' in r:
        # Extract snippet
        snippet = re.sub(r'<[^>]+>', ' ', r)
        snippet = re.sub(r'\s+', ' ', snippet).strip()
        print(f'\nSnippet: {snippet[:500]}')
        
        # Extract abstract
        abstract_m = re.search(r'class="c-abstract"[^>]*>(.*?)</div>', r, re.DOTALL)
        if abstract_m:
            abstract = re.sub(r'<[^>]+>', '', abstract_m.group(1)).strip()
            print(f'Abstract: {abstract[:500]}')
        
        # Extract content abstract
        content_ab = re.search(r'class="content-abstract"[^>]*>(.*?)</div>', r, re.DOTALL)
        if content_ab:
            content = re.sub(r'<[^>]+>', '', content_ab.group(1)).strip()
            print(f'Content Abstract: {content[:500]}')
        break
