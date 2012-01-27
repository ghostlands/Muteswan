#!/usr/bin/perl


#Copyright 2011-2012 James Unger,  Chris Churnick.
#This file is part of Muteswan.

#Muteswan is free software: you can redistribute it and/or modify
#it under the terms of the GNU Affero General Public License as published by
#the Free Software Foundation, either version 3 of the License, or
#(at your option) any later version.

#Muteswan is distributed in the hope that it will be useful,
#but WITHOUT ANY WARRANTY; without even the implied warranty of
#MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#GNU Affero General Public License for more details.

#You should have received a copy of the GNU Affero General Public License
#along with Muteswan.  If not, see <http://www.gnu.org/licenses/>.

use GD::Barcode::QRcode;


my $string = $ARGV[0];

if (!$string) {
	print "usage: genqrcode.pl string\n";
	exit;
}

binmode(STDOUT);
my $barcode = new GD::Barcode::QRcode($string, {'ModuleSize' => 5, 'Version' => 10});
print $barcode->plot->png;
