import urllib.request, urllib.error, ssl, re, json, http.cookiejar

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

cookie_jar = http.cookiejar.CookieJar()
opener = urllib.request.build_opener(
    urllib.request.HTTPSHandler(context=ctx),
    urllib.request.HTTPCookieProcessor(cookie_jar)
)

headers = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
    'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
    'Accept-Language': 'zh-CN,zh;q=0.9',
}

# Get cookies from zhihu.com first
req = urllib.request.Request('https://www.zhihu.com/', data=None, headers=headers)
resp = opener.open(req, timeout=15)
resp.read()

# Now try the zhuanlan API with cookies
api_headers = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
    'Accept': 'application/json, text/plain, */*',
    'Referer': 'https://zhuanlan.zhihu.com/',
    'x-requested-with': 'XMLHttpRequest',
}

# Try different API endpoints
endpoints = [
    'https://zhuanlan.zhihu.com/api/posts/4130861066',
    'https://www.zhihu.com/api/v4/posts/4130861066',
    'https://www.zhihu.com/api/v3/zhuanlan/posts/4130861066',
]

for url in endpoints:
    try:
        req = urllib.request.Request(url, data=None, headers=api_headers)
        resp = opener.open(req, timeout=10)
        data = json.loads(resp.read().decode('utf-8'))
        print(f'=== {url} ===')
        print(f'Status: {resp.status}')
        print(json.dumps(data, ensure_ascii=False, indent=2)[:2000])
        print()
    except urllib.error.HTTPError as e:
        print(f'{url}: HTTP {e.code}')
    except Exception as e:
        print(f'{url}: {type(e).__name__}: {str(e)[:100]}')
