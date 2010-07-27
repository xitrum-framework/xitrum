# This program migrates data and uploaded files from OpenKH 0.5 to LiKH
#
# Required gems:
#   dbi
#   dbd-pg
#   nokogiri

DB = {
  :adapter  => 'DBI:Pg',
  :host     => 'localhost',
  :port     => 5432,
  :username => 'postgres',
  :password => 'postgres',
  :openkh   => 'openkh',
  :colinh   => 'colinh'
}

require 'rubygems'
require 'yaml'
require 'dbi'
require 'nokogiri'  # For fixing malformed HTML

def fix_malformed_html(html)
  Nokogiri::HTML(html).xpath("//body").children.to_xml
end

#-------------------------------------------------------------------------------
# Helpers to play with PostgreSQL sequences

def currval(seq)
  $colinh.select_one("SELECT CASE WHEN is_called THEN last_value ELSE last_value-increment_by END from #{seq}")[0]
end

def setval(seq, val)
  $colinh.select_one("SELECT setval('#{seq}', #{val}, true)")
end

def convert_articles
  $openkh.select_all("SELECT * FROM node_versions INNER JOIN nodes ON nodes.type = 'Article' AND nodes.id = node_versions.node_id AND node_versions.version = nodes.active_version ORDER BY node_versions.id") do |a|
    title      = a[:title]
    obj        = YAML::load(a[:_body])
    teaser     = fix_malformed_html(obj[0])
    body       = fix_malformed_html(obj[1])
    hits       = 100
    created_at = Time.now.to_s
    updated_at = Time.now.to_s
    user_id    = 1

    $colinh.do("INSERT INTO article(title, teaser, body, sticky, hits, createdat, updatedat, userid) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
      title, teaser, body, false, hits, created_at, updated_at, user_id)
  end
end

#-------------------------------------------------------------------------------

# openkh -> colinh
$url_conversion_table = {}

def main
  $openkh = DBI.connect("#{DB[:adapter]}:#{DB[:openkh]}:#{DB[:host]}:#{DB[:port]}", DB[:username], DB[:password])
  $colinh = DBI.connect("#{DB[:adapter]}:#{DB[:colinh]}:#{DB[:host]}:#{DB[:port]}", DB[:username], DB[:password])

#  $colinh.do('DELETE FROM user_');             setval('user__id_seq',         1)
#  $colinh.do('DELETE FROM category');           setval('category_id_seq',       1)
  $colinh.do('DELETE FROM article');            setval('article_id_seq',        1)
#  $colinh.do('DELETE FROM articlescategories')
#  $colinh.do('DELETE FROM comment_t');          setval('comment_t_id_seq',      1)

  convert_articles

  $openkh.disconnect
  $colinh.disconnect
end
main
