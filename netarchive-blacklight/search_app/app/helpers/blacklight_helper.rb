require 'cgi'

module BlacklightHelper
  include Blacklight::BlacklightHelperBehavior

  def link_to_document (doc, opts={:label=>nil, :counter => nil})
    waybackURL = blacklight_config.wayback_url || ''

    # run through all keys in the configured URL and replace them
    # with values from the current document 
    actualURL = waybackURL;
    expando = waybackURL.scan(/\{\w+\}/)
    expandoURLEncode = waybackURL.scan(/\[\w+\]/)

    # replace ordinary keys
    expando.each do
        |exp|
        key = exp.gsub(/[{}]/, '')
        value = doc[key]
        actualURL = actualURL.gsub('{'+key+'}', value)
    end

    # replace keys that need to be url encoded
    expandoURLEncode.each do
        |exp|
        key = exp.gsub(/[\[\]]/, '')
        value = doc[key]
        value = CGI::escape(value)
        actualURL = actualURL.gsub('['+key+']', value)
    end

    title = 'No title found'
    if doc['title'] != nil
        title = doc['title'][0]
    end

    link_to title, actualURL
  end
end
