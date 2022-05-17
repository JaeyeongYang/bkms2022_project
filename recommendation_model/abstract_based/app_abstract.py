# Abstract 기반 추천 코드입니다. 아래 패키지들이 전부 설치되어 있어야 실행 가능합니다.
# json 경로는 알맞게 수정하셔서 사용하기 바랍니다.

import flask
import csv
from flask import Flask, render_template, request
import difflib
import pandas as pd
from sklearn.feature_extraction.text import CountVectorizer
from sklearn.metrics.pairwise import cosine_similarity
import random
import json

title_lst, year_lst, abs_list = [], [], []
with open('./Data/part1.json', encoding="utf-8") as f:
  data = json.load(f)
# Create 'Title' DataFrame
    
for i in data:
    if 'abstract' in i:
        json_title = i['title']
        json_date = i['year']
        json_abs = i['abstract']
        title_lst.append(json_title)
        year_lst.append(json_date)
        abs_list.append(json_abs)

df = pd.DataFrame(year_lst, columns = ['year'])
df0 = pd.DataFrame(abs_list, columns = ['abstract'])
df1 = pd.DataFrame(title_lst, columns = ['title'])
df2 = pd.concat([df,df0, df1],axis=1)
count = CountVectorizer(stop_words='english')
count_matrix = count.fit_transform(df2['abstract'])

df2 = df2.reset_index()
indices = pd.Series(df2.index, index=df2['title'])
all_titles = [df2['title'][i] for i in range(len(df2['title']))]

def get_recommendations(title):
    cosine_sim = cosine_similarity(count_matrix, count_matrix)
    idx = indices[title]
    sim_scores = list(enumerate(cosine_sim[idx]))
    sim_scores = sorted(sim_scores, key=lambda x: x[1], reverse=True)
    sim_scores = sim_scores[1:11]
    movie_indices = [i[0] for i in sim_scores]
    tit = df2['title'].iloc[movie_indices]
    dat = df2['year'].iloc[movie_indices]
    return_df = pd.DataFrame(columns=['Title', 'Year'])
    return_df['Title'] = tit
    return_df['Year'] = dat
    return return_df

import flask
app = flask.Flask(__name__, template_folder='templates')
# Set up the main route
@app.route('/', methods=['GET', 'POST'])
def main():
  if flask.request.method == 'GET':
    return(flask.render_template('index.html'))
  if flask.request.method == 'POST':
    pname = flask.request.form['paper_name']
    pname = pname.title()
    if pname not in all_titles:
      return(flask.render_template('negative.html', name=pname))
    else:
      result_final = get_recommendations(pname)
      names, dates = [], []
      for i in range(len(result_final)):
        names.append(result_final.iloc[i][0])
        dates.append(result_final.iloc[i][1])
      return flask.render_template('positive.html', paper_names=names, paper_date=dates, search_name=pname)
    
if __name__ == '__main__':
    app.run()
