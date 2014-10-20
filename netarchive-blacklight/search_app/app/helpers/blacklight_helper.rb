require 'cgi'

module BlacklightHelper
  include Blacklight::BlacklightHelperBehavior

  def link_to_document (doc, opts={:label=>nil, :counter => nil})
    waybackBaseURL = 'http://localhost:8080/showmebyid'
    title = 'No title found'
    if doc['title'] != nil
        title = doc['title'][0]
    end

    link_to title, waybackBaseURL + '?id=' + CGI::escape(doc['id'])
  end
end
