import urllib.request, urllib.error, ssl, re, json

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

url = 'https://cn.bing.com/search?q=zhuanlan.zhihu.com%2Fp%2F4130861066'
req = urllib.request.Request(url, data=None, headers={
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
    'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
    'Accept-Language': 'zh-CN,zh;q=0.9',
})
resp = urllib.request.urlopen(req, timeout=10, context=ctx)
text = resp.read().decode('utf-8', errors='replace')

# Save for inspection
with open('F:\\cy\\hermes\\demo1\\bing_search.html', 'w', encoding='utf-8') as f:
    f.write(text)

# Extract search result titles and snippets
# Bing uses <li class="b_algo"> for results
results = re.findall(r'<li class="b_algo"[^>]*>(.*?)</li>', text, re.DOTALL)
print(f'Found {len(results)} results')

for i, r in enumerate(results[:5]):
    # Title
    title_m = re.search(r'<h2>(.*?)</h2>', r, re.DOTALL)
    title = ''
    if title_m:
        title = re.sub(r'<[^>]+>', '', title_m.group(1)).strip()
    
    # URL
    url_m = re.search(r'<a[^>]*href="(https?://[^"]+)"', r)
    link = url_m.group(1) if url_m else ''
    
    # Snippet
    snippet_m = re.search(r'<p[^>]*>(.*?)</p>', r, re.DOTALL)
    snippet = ''
    if snippet_m:
        snippet = re.sub(r'<[^>]+>', '', snippet_m.group(1)).strip()
    
    print(f'\n--- Result {i+1} ---')
    print(f'Title: {title}')
    print(f'URL: {link}')
    print(f'Snippet: {snippet[:300]}')

# Also try to find the page title
title_tag = re.search(r'<title>(.*?)</title>', text)
if title_tag:
    print(f'\n\nPage Title: {title_tag.group(1)}')
