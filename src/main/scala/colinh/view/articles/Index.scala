package colinh.view.articles

import xt.framework.CV
import colinh.model.Article

class Index extends CV {
  def render = {
    at("title", "Colinh Home")

    val articles = at[List[Article]]("articles")
    <ul>
      {articles.map(a => <li>{renderArticle(a)}</li>)}
    </ul>
  }

  private def renderArticle(article: Article) =
    <div class="article">
      <h1>{article.title}</h1>
      <div>{article.teaser}</div>
    </div>
}
