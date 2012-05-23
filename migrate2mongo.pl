#!/usr/bin/perl

use HTTP::Date qw(time2str);


sub get_message {
        my $file = shift;
        my $jsonString;
        open(FILE,"$file") || return;
        my($mtime) = (stat FILE)[9];
        my $jsonString = <FILE>;
        close(FILE);
        return($jsonString,time2str($mtime));
}

sub getIndex {
        my $dir = shift;
        my @msgs = getMsgs($dir);
        return(pop(@msgs));
}

sub getMsgs {
        my $path = shift;
        opendir(DIR,$path);
        my @msgs = sort {$a <=> $b} grep { !/^\.$/ && !/^\.\.$/ && !/manifest/} readdir(DIR);
        closedir(DIR);
        return(@msgs);
}



my $outputdir = $ARGV[0];
my $arg = $ARGV[1];

if ($arg =~ /(.*?)\/(\w+)\/(\d+)$/) {
	my $hash = "C" . $2;
	my $id = $3;
	my ($msg,$timestamp) = get_message($arg);

	open(CIRCLE,">>$outputdir/$hash" . ".json");

	#print "db.$hash.save( { \"_id\": $id, \"timestamp\": \"$timestamp\", \"content\": $msg } )\n";
	print CIRCLE "{ \"_id\": $id, \"timestamp\": \"$timestamp\", \"content\": $msg } \n";
	close(CIRCLE);
} elsif ($arg =~ /(\w+)$/ && $arg !~ /manifest/ && !$arg !~ /ex/) {
	my $lastMsg = getIndex($arg);
	open(COUNTERS,">>$outputdir/counters" . ".json");
	#print "db.counters.save( { \"_id\": " . 'C' . $1 . ", \"n\": $lastMsg } )\n";
	print COUNTERS " { \"_id\": \"" . 'C' . $1 . "\", \"n\": $lastMsg } \n";
	close(COUNTERS);
}

