import urllib.request, urllib.error, ssl, re, json

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

# Try multiple proxy/reader services
urls = [
    'https://r.jina.ai/https://zhuanlan.zhihu.com/p/4130861066',
    'https://api.allorigins.win/raw?url=https://zhuanlan.zhihu.com/p/4130861066',
    'https://textise.iitty.com/show?url=https://zhuanlan.zhihu.com/p/4130861066',
]

for url in urls:
    try:
        req = urllib.request.Request(url, data=None, headers={
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
        })
        resp = urllib.request.urlopen(req, timeout=20, context=ctx)
        text = resp.read().decode('utf-8', errors='replace')
        svc = url.split('/')[2]
        print(f'=== {svc} ===')
        print(f'Status: {resp.status} Length: {len(text)}')
        print(text[:800])
        print('---')
        if len(text) > 2000 and 'CAPTCHA' not in text and '安全验证' not in text:
            with open('F:\\cy\\hermes\\demo1\\zhihu_content.txt', 'w', encoding='utf-8') as f:
                f.write(text)
            print('>>> SAVED!')
            break
    except Exception as e:
        svc = url.split('/')[2]
        print(f'{svc}: {type(e).__name__}: {str(e)[:100]}')
