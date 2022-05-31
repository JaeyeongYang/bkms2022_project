# 파이썬에서 postgresql로 임베딩 벡터 보내 저장하고 가져와서 계산
# 빅쿼리와 달리 array를 저장할 수있는 postgresql의 장점을 활용함 

# postgresql package
import psycopg2
import sqlalchemy
from sqlalchemy import create_engine

# tf_hub
import tensorflow as tf
import tensorflow_hub as hub

# basics
import numpy as np
import json
import pandas as pd
from pandas import DataFrame

# Postgresql 접근 
engine = create_engine("postgresql://bkms:bkmspostgres@ccsl1.snu.ac.kr:54006/postgres")
conn = engine.connect()

# 미리 학습된 인코더 불러오기 
embed = hub.load("https://tfhub.dev/google/universal-sentence-encoder/4", tags=None, options=None)
'''
링크를 통해서 불러오는 것이 느릴 경우 아래 코드와 같이 학습된 인코더를 다운받아 사용 가능
다운로드 링크 : https://tfhub.dev/google/universal-sentence-encoder/4
import tarfile
file=tarfile.open('./universal-sentence-encoder_4.tar.gz')
file.extractall('./sent4')
'''

# neo4j에서 데이터를 가져와 df로 정리했다고 가정(neo4j에서 데이터 가져오는 핸들러 필요), title과 pub2.key의 두가지 칼럼만 가짐
embeddings = embed(df['title'])
use = np.array(embeddings).tolist()
df['embeddings'] = use

# execute 실행시 오류가 나는 경우가 잦아 미리 실행시킴
# postgresql 관련 코드에서 오류가 날 경우 코드 앞에 다 갖다 붙임 
cur.execute("ROLLBACK")
con.commit()

# 임베딩 저장할 테이블 생성 쿼리 
cur.execute('''
DROP TABLE IF EXISTS embeds;
CREATE TABLE embeds(
    pub2.key VARCHAR(30),
    embeddings Float8[][])
''')
con.commit()

df2 = df.loc[:, ["pub2.key", "embeddings"]] # 타이틀 제거

# pub2.key, embeddings를 postgresql로 보냄
df2.to_sql('prac4', con=conn, if_exists='append',
          index=False)

# 계산 결과(score) 저장할 테이블 생성 쿼리 
cur.execute('''
DROP TABLE IF EXISTS result;
CREATE TABLE result(
    pub2.key VARCHAR(30),
    Score INT4)
''')
con.commit()

# 논문의 고유 key를 입력받으면 결과를 도출하는 것으로 수정함 
def get_recommendations(pub2.key):
    
    # 저장한 임베딩 벡터 불러오기(20개)
    query = """
        SELECT pub2.key, embeddings
        FROM embeds
        LIMIT 20;
        """
    qur = execute(query)
    result = pd.DataFrame(qur, columns = ['pub2.key', 'embeddings'])
  
    # key, embedding 벡터 리스트 생성
    key_lst = list(result['pub2.key'])
    key_num = key_lst.index(pub2.key)
    emb_lst = list(result['embeddings'])
    k_lst, score_lst = [], []
    for i in range(len(key_lst)):
        k_lst.append(key_lst[i])
        # 계산 안될 경우 tf.tensor로 변환 후 진행 
        score_lst.append(np.inner(emb_lst[key_num], emb_lst[i]))
    
    # 입력받은 값은 삭제하고 결과 냄
    del k_lst[key_num]
    del score_lst[key_num]
    dft = pd.DataFrame(k_lst, columns = ['Key'])
    dfs = pd.DataFrame(score_lst, columns = ['Score'])
    return_df = pd.concat([dft,dfs],axis=1)
    
    # 결과 postgresql에 보냄
    return_df.to_sql(name = 'embed_score',
          con = conn,
          schema = 'public',
          if_exists = 'append',
          index = False)
