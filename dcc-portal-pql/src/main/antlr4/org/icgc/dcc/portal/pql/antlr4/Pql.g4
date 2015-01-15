grammar Pql;

program  
 : EOF
 | query (BOOLOP_SHORT query)* EOF
 ;

query
 : filter
 | function
 | function (BOOLOP_SHORT filter)* (BOOLOP_SHORT function)*
 | filter (BOOLOP_SHORT filter)* (BOOLOP_SHORT function)*
 ;

function
 : 'count' OPAR CPAR					# count
 | 'distinct' OPAR CPAR					# distinct
 | 'first' OPAR CPAR					# first
 | 'limit' OPAR INT CPAR				# limit
 | 'select' OPAR ID (COMMA ID)* CPAR	# select
 | 'sort' OPAR SORT_ORDER ID CPAR		# sort
 ;

filter
 : 'eq' OPAR ID COMMA VALUE CPAR						# eq 
 | 'ne' OPAR ID COMMA VALUE CPAR						# ne
 | 'gt' OPAR ID COMMA VALUE CPAR						# gt
 | 'ge' OPAR ID COMMA VALUE CPAR						# ge
 | 'lt' OPAR ID COMMA VALUE CPAR						# lt
 | 'le' OPAR ID COMMA VALUE CPAR						# le
 | 'in' OPAR ID COMMA VALUE_ARRAY CPAR					# in
 | 'and' OPAR filter COMMA filter (COMMA filter)* CPAR	# and
 | 'or' OPAR filter COMMA filter (COMMA filter)* CPAR	# or
 ;

VALUE
 : STRING
 | INT
 | FLOAT
 ;

SORT_ORDER
 : PLUS
 | MINUS
 ;

VALUE_ARRAY
 : OBRK VALUE* CBRK
 ;

BOOLOP_SHORT 
 : '&' 
 | '|'
 ;

OR : '||';
AND : '&&';
EQ : '==';
NEQ : '!=';
GT : '>';
LT : '<';
GTEQ : '>=';
LTEQ : '<=';
PLUS : '+';
MINUS : '-';
ASSIGN : '=';
OBRK : '[';
CBRK : ']';
OPAR : '(';
CPAR : ')';
COMMA : ',';

TRUE : 'true';
FALSE : 'false';

ID
 : [a-zA-Z_] [a-zA-Z_0-9]*
 ;


INT
 : [0-9]+
 ;

FLOAT
 : [0-9]+ '.' [0-9]* 
 | '.' [0-9]+
 ;

SPACE
 : [ \t\r\n] -> skip
 ;

STRING
 : '"' (~["\r\n] | '""')* '"'
 ;

OTHER
 : . 
 ;