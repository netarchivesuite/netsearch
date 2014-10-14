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

Now to run the server do `rails server` and open [http://localhost:3000/](http://localhost:3000/)

