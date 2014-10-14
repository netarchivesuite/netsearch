# Blacklight

This is a very simple [Blacklight](http://projectblacklight.org/) installation to search the netarchive.

## Prerequisites
Needs Ruby 1.9 or newer, Rails 4 (or 3.2) and sqlite3 with development headers installed as necessary.

To install these on Fedora do the following

`yum install ruby ruby-devel rubygem-rails sqlite sqlite-devel`

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
- [ ] Cleanup unneeded things from templates
  - [ ] Bookmarks
  - [ ] Sort dropdown
  - [ ] Search field dropdown
  - [ ] Login
  - [ ] History
- [ ] Adjust logo
- [ ] Hit highlighting on the content_text field
- [ ] Some form of grouping
