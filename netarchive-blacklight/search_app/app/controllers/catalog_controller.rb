# -*- encoding : utf-8 -*-
#
class CatalogController < ApplicationController  
  include Blacklight::Marc::Catalog

  include Blacklight::Catalog

  configure_blacklight do |config|
    ## Default parameters to send to solr for all search-like requests. See also SolrHelper#solr_search_params
    config.default_solr_params = { 
      :qt => 'search',
      :rows => 10#, 
      #:hl => 'true',
      #:hl.field => 'content_text'
    }
    
    # solr path which will be added to solr base url before the other solr params.
    #config.solr_path = 'select' 
    
    # items to show per page, each number in the array represent another option to choose from.
    config.per_page = [10,20,50,100]

    # solr field configuration for search results/index views
    config.index.title_field = 'title'

    # solr field configuration for document/show views
    config.show.title_field = 'title'

    # solr fields that will be treated as facets by the blacklight application
    # The ordering of the field names is the order of the display
    config.add_facet_field 'domain', :label => 'Domain'
    config.add_facet_field 'crawl_year', :label => 'Crawl Year', :single => true, sort: 'index'
    config.add_facet_field 'content_type_norm', :label => 'Content Type' 
    config.add_facet_field 'host', :label => 'Host'
    config.add_facet_field 'public_suffix', :label => 'Public Suffix'
    config.add_facet_field 'url', :label => 'URL'

    # Have BL send all facet field names to Solr, which has been the default
    # previously. Simply remove these lines if you'd rather use Solr request
    # handler defaults, or have no facets.
    config.add_facet_fields_to_solr_request!

    # solr fields to be displayed in the index (search results) view
    # The ordering of the field names is the order of the display 
    config.add_index_field 'content_type', :label => 'Content Type'
    config.add_index_field 'crawl_date', :label => 'Crawl Date'

    # solr fields to be displayed in the show (single result) view
    #   The ordering of the field names is the order of the display 
    config.add_show_field 'title', :label => 'Title'

    # "fielded" search configuration. Used by pulldown among other places.
    config.add_search_field 'all_fields', :label => 'All Fields'
    

    # Now we see how to over-ride Solr request handler defaults, in this
    # case for a BL "search field", which is really a dismax aggregate
    # of Solr search fields. 
    
    #config.add_search_field('title') do |field|
    #  # solr_parameters hash are sent to Solr as ordinary url query params. 
    #  field.solr_parameters = { :'spellcheck.dictionary' => 'title' }

    #  # :solr_local_parameters will be sent using Solr LocalParams
    #  # syntax, as eg {! qf=$title_qf }. This is neccesary to use
    #  # Solr parameter de-referencing like $title_qf.
    #  # See: http://wiki.apache.org/solr/LocalParams
    #  field.solr_local_parameters = { 
    #    :qf => '$title_qf',
    #    :pf => '$title_pf'
    #  }
    #end
    
    #config.add_search_field('author') do |field|
    #  field.solr_parameters = { :'spellcheck.dictionary' => 'author' }
    #  field.solr_local_parameters = { 
    #    :qf => '$author_qf',
    #    :pf => '$author_pf'
    #  }
    #end
    
    # "sort results by" select (pulldown)
    # label in pulldown is followed by the name of the SOLR field to sort by and
    # whether the sort is ascending or descending (it must be asc or desc
    # except in the relevancy case).
    config.add_sort_field 'score desc', :label => 'relevance'
  end

end 
