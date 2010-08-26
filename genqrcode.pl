#!/usr/bin/perl


use GD::Barcode::QRcode;


my $string = $ARGV[0];

if (!$string) {
	print "usage: genqrcode.pl string\n";
	exit;
}

binmode(STDOUT);
my $barcode = new GD::Barcode::QRcode($string, {'ModuleSize' => 5, 'Version' => 10});
print $barcode->plot->png;
