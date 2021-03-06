IndexName=studio_test
IP=52.191.253.236:9200

# create index's mappings
curl -X PUT "$IP/$IndexName" -H 'Content-Type: application/json' -d'
{
  "mappings": {
    "qna": {
      "properties": {
        "question": {
          "type": "text"
        },
        "answer": {
          "type": "text"
        },
		"labeledQuestion":{
		  "type": "text"
		},
        "keywords": {
          "type": "text",
		  "store": true
        },
        "embedding": {
          "type": "float",
		  "store": true
        }
      }
    }
  }
}
'
# check mappings
curl -X GET "$IP/$IndexName/_mapping/qna" -H 'Content-Type: application/json' -d'
{
}
'

# index some test samples
curl -X POST "$IP/$IndexName/qna" -H 'Content-Type: application/json' -d'
{
  "question": "Office 2013在哪下载？",
  "answer": "您可以通过office入口进行下载安装office。",
  "labeledQuestion":"<kw>Office 2013</kw>在哪<kw>下载</kw>？",
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
curl -X POST "$IP/$IndexName/qna" -H 'Content-Type: application/json' -d'
{
  "question": "mac的excel怎么登陆",
  "answer": "您可以通过左上角账户入口进行登录",
  "labeledQuestion": "<kw>mac</kw>的<kw>excel</kw>怎么登陆",
  "keywords": [
  ],
  "embedding": [
    0.32,
	1.62,
	43.64
  ]
}
'
curl -X POST "$IP/$IndexName/qna" -H 'Content-Type: application/json' -d'
{
  "question": "pdf文字怎么粘贴进word",
  "answer": "您可以通过pdf阅读软件选取字体复制，然后粘贴进word",
  "labeledQuestion": "<kw>pdf</kw>文字怎么粘贴进<kw>word</kw>",
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
      "question": "office怎么下载"
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