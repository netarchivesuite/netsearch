# Blacklight

This is a very simple [Blacklight](http://projectblacklight.org/) installation to search the netarchive.

## Prerequisites
Needs Ruby 1.9 or newer, Rails 4 (or 3.2) and sqlite3 with development headers installed as necessary. Also needs a javascript interpreter supported by Ruby - nodejs should do fine.

### Fedora
To install these on Fedora do the following

`yum install ruby ruby-devel rubygem-rails sqlite sqlite-devel nodejs`

### Ubuntu
To install these on Ubuntu do the following

`sudo apt-get install ruby-dev ruby-rails-4.0 libsqlite3-dev sqlite`

Then install nodejs for Ruby

`bundler nodejs`

Since Ubuntu currently has Ruby 1.9.1 also install `rdoc-data`

```
sudo gem install rdoc-data
sudo rdoc-data --install
```

If it still does not work then try the following

`sudo gem install rails`

## Setup
```
cd search_app
bundle install
rake db:migrate
```

That should install all the needed parts for Blacklight and setup its database.

Edit `config/solr.yml` to point to your Netarchive Solr instance.

Now to run the server do `rails server` and open [http://localhost:3000/](http://localhost:3000/)

## Configuration
Configuration of fields and facets to be shown, Solr params, etc. can be found in `app/controllers/catalog_controller.rb`.

Individual methods can be overwritten in `app/helpers/blacklight_helper.rb` (as has for instance been done with `link_to_document`in order to make it point to our Wayback instance).

Read more about [configuring Blacklight](https://github.com/projectblacklight/blacklight/wiki/Blacklight-configuration)

## TODO
- [x] Simplify config
- [x] Cleanup unneeded things from templates
  - [x] Bookmarks (hidden in CSS)
  - [ ] Sort dropdown
  - [x] Search field dropdown (hidden in CSS)
  - [x] Login (hidden in CSS)
  - [x] History (hidden in CSS)
- [x] Adjust logo
- [x] Hit highlighting on the content_text field
- [ ] Some form of grouping on url (see notes in `app/controllers/catalog_controller.rb`)
