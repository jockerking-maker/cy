import urllib.request, urllib.error, ssl, re, json

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

# Try Google cache
url = 'https://webcache.googleusercontent.com/search?q=cache:https://zhuanlan.zhihu.com/p/4130861066&strip=1&vwsrc=0'
try:
    req = urllib.request.Request(url, data=None, headers={
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
    })
    resp = urllib.request.urlopen(req, timeout=15, context=ctx)
    text = resp.read().decode('utf-8', errors='replace')
    print(f'Google Cache: Status {resp.status} Length {len(text)}')
    if len(text) > 2000:
        # Extract just the content
        m = re.search(r'<div[^>]*id="google-cache"[^>]*>(.*?)</div>', text, re.DOTALL)
        if m:
            print('Found cache div')
        # Look for article content
        content_match = re.search(r'class="Post-RichText[^"]*"[^>]*>(.*?)</div>', text, re.DOTALL)
        if content_match:
            print(f'Found RichText: {len(content_match.group(1))} chars')
        print(text[:2000])
        with open('F:\\cy\\hermes\\demo1\\zhihu_cache.html', 'w', encoding='utf-8') as f:
            f.write(text)
        print('Saved!')
    else:
        print(text[:500])
except Exception as e:
    print(f'Google Cache Error: {type(e).__name__}: {e}')

# Try Bing cache
url2 = 'https://cc.bingj.com/cache.aspx?d=4&s=AGHIJkFmGkGklkGklkGklkGklkGk&d=1&w=abc'
try:
    req = urllib.request.Request(url2, data=None, headers={
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
    })
    resp = urllib.request.urlopen(req, timeout=10, context=ctx)
    print(f'Bing: {resp.status}')
except Exception as e:
    print(f'Bing Error: {type(e).__name__}: {e}')
