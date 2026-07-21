import urllib.request, urllib.error, ssl, re, json

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

# Try DuckDuckGo lite to search for this article
url = 'https://lite.duckduckgo.com/lite/?q=zhuanlan.zhihu.com+p+4130861066'
try:
    req = urllib.request.Request(url, data=None, headers={
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
    })
    resp = urllib.request.urlopen(req, timeout=10, context=ctx)
    text = resp.read().decode('utf-8', errors='replace')
    print(f'DDG: {len(text)} chars')
    # Extract result links
    links = re.findall(r'<a[^>]*href="([^"]+)"[^>]*class="result-link"[^>]*>(.*?)</a>', text, re.DOTALL)
    for href, title in links[:5]:
        print(f'  {title.strip()} -> {href}')
    # Also extract plain text
    text_content = re.sub(r'<[^>]+>', ' ', text)
    text_content = re.sub(r'\s+', ' ', text_content).strip()
    print(text_content[:1000])
except Exception as e:
    print(f'DDG Error: {type(e).__name__}: {e}')

print('\n---\n')

# Try Bing search
url2 = 'https://www.bing.com/search?q=zhuanlan.zhihu.com%2Fp%2F4130861066'
try:
    req = urllib.request.Request(url2, data=None, headers={
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
        'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
        'Accept-Language': 'zh-CN,zh;q=0.9',
    })
    resp = urllib.request.urlopen(req, timeout=10, context=ctx)
    text = resp.read().decode('utf-8', errors='replace')
    print(f'Bing: {len(text)} chars')
    # Extract titles
    titles = re.findall(r'<h2>(.*?)</h2>', text, re.DOTALL)
    for t in titles[:5]:
        clean = re.sub(r'<[^>]+>', '', t)
        print(f'  {clean}')
    # Check if we got redirected
    print(f'URL: {resp.url}')
except Exception as e:
    print(f'Bing Error: {type(e).__name__}: {e}')
