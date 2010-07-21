package colinh.view.articles

import xt.framework.Env
import colinh.model.Article

class Index extends Env {
  def render = {
    val articles = params[List[Article]]("articles")
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
