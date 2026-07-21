import urllib.request, urllib.error, ssl, re, json

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

# Try Wayback Machine CDX API for this URL
url = 'https://web.archive.org/cdx/search/cdx?url=zhuanlan.zhihu.com/p/4130861066&output=json&limit=10'
try:
    req = urllib.request.Request(url, data=None, headers={
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
    })
    resp = urllib.request.urlopen(req, timeout=15, context=ctx)
    data = json.loads(resp.read().decode('utf-8'))
    print(f'Wayback results: {len(data)} entries')
    for entry in data[:5]:
        print(entry)
except Exception as e:
    print(f'Wayback CDX Error: {type(e).__name__}: {e}')

print('\n---\n')

# Try to search for this article on Zhihu
url2 = 'https://www.zhihu.com/search?type=content&q=4130861066'
try:
    req = urllib.request.Request(url2, data=None, headers={
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
        'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
        'Accept-Language': 'zh-CN,zh;q=0.9',
    })
    resp = urllib.request.urlopen(req, timeout=10, context=ctx)
    text = resp.read().decode('utf-8', errors='replace')
    print(f'Search: {len(text)} chars')
    # Extract title
    m = re.search(r'<title>(.*?)</title>', text)
    if m:
        print(f'Title: {m.group(1)}')
    print(text[:500])
except Exception as e:
    print(f'Search Error: {type(e).__name__}: {e}')
