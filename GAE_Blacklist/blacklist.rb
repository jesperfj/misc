#!/usr/bin/ruby
require "open-uri.rb"

all = {}
open("http://java.sun.com/javase/6/docs/api/allclasses-frame.html") { |f|
  f.each_line { |line|
    if line.sub!(/^<A HREF=\"([^\"]*)\".*$/) { $1 }
      line.gsub!(/\//) { "." }
      line.sub!(/\.html/) { "" }
      all[line] = line
    end
  }
} 
unknown = {}
n_all = all.size
n_white = 0
started = false
open("http://code.google.com/appengine/docs/java/jrewhitelist.html") { |f|
  f.each_line { |line|
    if line =~ /<h1 class="page_title">The JRE Class White List<\/h1>/
        started = true
    end
    if started
      if line.sub!(/^ *<li>(.*)<\/li>/) { $1 }
        n_white += 1
        unless all.delete(line)
          unknown[line] = line
        end
      end
    end 
  }
}
puts "BLACKLISTED CLASSES ============================================="
puts
all.keys.sort.each { |key| puts key }
puts 
puts "WHITELISTED BUT NOT IN JDK ======================================"
puts
unknown.keys.sort.each { |key| puts key }
puts 

puts "#{n_all} total classes. #{n_white} whitelisted classes. #{all.size} blacklisted classes. #{unknown.size} mismatched classes"
