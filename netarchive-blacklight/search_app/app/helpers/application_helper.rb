module ApplicationHelper

  def get_url_link args
    url = args[:document][args[:field]]
    short_url = url
    if short_url.length > 50
      short_url = short_url[0..20] + '...' + short_url[-20..-1]
    end

    link_to short_url, url
  end

end
