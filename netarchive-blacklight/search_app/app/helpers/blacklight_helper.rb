module BlacklightHelper
  include Blacklight::BlacklightHelperBehavior

  def link_to_document (doc, opts={:label=>nil, :counter => nil})
    link_to doc['title'][0], 'http://localhost:8080/showme?id=' + doc['id']
  end
end
