import urllib.request, urllib.error, ssl, re, json

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

# Try via textise proxy
urls = [
    'https://r.jina.ai/http://zhuanlan.zhihu.com/p/4130861066',
    'https://corsproxy.io/?url=https://zhuanlan.zhihu.com/p/4130861066',
]

for url in urls:
    try:
        req = urllib.request.Request(url, data=None, headers={
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
        })
        resp = urllib.request.urlopen(req, timeout=20, context=ctx)
        text = resp.read().decode('utf-8', errors='replace')
        print(f'=== {url.split("/")[2]} ===')
        print(f'Status: {resp.status} Length: {len(text)}')
        print(text[:1000])
        print('...')
        if len(text) > 2000:
            with open('F:\\cy\\hermes\\demo1\\zhihu_content.txt', 'w', encoding='utf-8') as f:
                f.write(text)
            print('Saved!')
            break
    except Exception as e:
        print(f'{url.split("/")[2]}: {type(e).__name__}: {e}')
