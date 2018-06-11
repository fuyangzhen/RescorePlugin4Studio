IndexName=studio_test
IP=52.191.253.236:9200

# create index's mappings
curl -X PUT "$IP/$IndexName" -H 'Content-Type: application/json' -d'
{
  "mappings": {
    "_doc": {
      "properties": {
        "q": {
          "type": "text"
        },
        "a": {
          "type": "text"
        },
        "keywords": {
          "type": "keyword"
        },
        "embedding": {
          "type": "float"
        }
      }
    }
  }
}
'
# check mappings
curl -X GET "$IP/$IndexName/_mapping/_doc" -H 'Content-Type: application/json' -d'
{
}
'

# index some test samples
curl -X POST "$IP/$IndexName/_doc" -H 'Content-Type: application/json' -d'
{
  "q": "Office 2013在哪下载？",
  "a": "您可以通过office入口进行下载安装office。",
  "keywords": [
    "office",
	"2013"
  ],
  "embedding": [
    7.32,
	5.62,
	4.64
  ]
}
'
curl -X POST "$IP/$IndexName/_doc" -H 'Content-Type: application/json' -d'
{
  "q": "mac的excel怎么登陆",
  "a": "您可以通过左上角账户入口进行登录",
  "keywords": [
  ],
  "embedding": [
    0.32,
	1.62,
	43.64
  ]
}
'
curl -X POST "$IP/$IndexName/_doc" -H 'Content-Type: application/json' -d'
{
  "q": "pdf文字怎么粘贴进word",
  "a": "您可以通过pdf阅读软件选取字体复制，然后粘贴进word",
  "keywords": [
    "pdf",
	"word",
	"粘贴"
  ],
  "embedding": [
  ]
}
'

# search by plugin
curl -X POST "$IP/$IndexName/_search" -H 'Content-Type: application/json' -d'
{
  "query": {
    "match": {
      "q": "office怎么下载"
    }
  },
  "rescore": {
    "plugin_parameters": {
      "queryFactors": [
        0.3,
        0.2,
        5
      ],
      "queryKeywords": [
        "office",
        "下载"
      ],
      "queryEmbedding": [
        7.31,
        5.6
      ]
    }
  }
}
'