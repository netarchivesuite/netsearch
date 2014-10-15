require 'cgi'

module BlacklightHelper
  include Blacklight::BlacklightHelperBehavior

  def link_to_document (doc, opts={:label=>nil, :counter => nil})
    waybackURL = 'http://elara.statsbiblioteket.dk/wayback/{wayback_date}/{url}'

    # run through all {} keys in the configured URL and replace them
    # with values from the current document 
    actualURL = waybackURL;
    expando = waybackURL.scan(/\{\w+\}/)
    expando.each {
        |exp|
        key = exp.gsub(/[{}]/, '')
        actualURL = actualURL.gsub('{'+key+'}', doc[key])
    }

    title = 'No title found'
    if doc['title'] != nil
        title = doc['title'][0]
    end

    link_to title, actualURL
  end
end
