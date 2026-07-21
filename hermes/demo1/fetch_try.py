import urllib.request, urllib.error, ssl, re, json

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

# Try to get the article title and content via Zhihu's public API
# First, let's try to find what this article is about by searching for the URL pattern
# The URL https://zhuanlan.zhihu.com/p/4130861066 - let's check if it exists

# Try 1: Use textise dot iitty
try:
    url = 'https://r.jina.ai/http://zhuanlan.zhihu.com/p/4130861066'
    req = urllib.request.Request(url, data=None, headers={
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
        'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
    })
    resp = urllib.request.urlopen(req, timeout=15, context=ctx)
    text = resp.read().decode('utf-8', errors='replace')
    print(f'Jina: {len(text)} chars')
    print(text[:500])
except Exception as e:
    print(f'Jina Error: {e}')

print('\n---\n')

# Try 2: Use textise dot iitty
try:
    url = 'https://r.jina.ai/https://zhuanlan.zhihu.com/p/4130861066'
    req = urllib.request.Request(url, data=None, headers={
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
    })
    resp = urllib.request.urlopen(req, timeout=15, context=ctx)
    text = resp.read().decode('utf-8', errors='replace')
    print(f'Jina https: {len(text)} chars')
    print(text[:500])
except Exception as e:
    print(f'Jina https Error: {e}')
