// Grammaire du langage PROJET
// COMP L3  
// Anne Grazon, V�ronique Masson
// il convient d'y ins�rer les appels � {PtGen.pt(k);}
// relancer Antlr apr�s chaque modification et raffraichir le projet Eclipse le cas �ch�ant

// attention l'analyse est poursuivie apr�s erreur si l'on supprime la clause rulecatch

grammar projet;

options {
  language=Java; k=1;
 }

@header {           
import java.io.IOException;
import java.io.DataInputStream;
import java.io.FileInputStream;
} 


// partie syntaxique :  description de la grammaire //
// les non-terminaux doivent commencer par une minuscule


@members {

 
// variables globales et m�thodes utiles � placer ici
  
}
// la directive rulecatch permet d'interrompre l'analyse � la premi�re erreur de syntaxe
@rulecatch {
catch (RecognitionException e) {reportError (e) ; throw e ; }}


unite  :   unitprog  EOF
      |    unitmodule  EOF
  ;
  
unitprog
  : 'programme' ident ':'  
     declarations  
     corps { System.out.println("succ�s, arret de la compilation "); }
  ;
  
unitmodule
  : 'module' ident ':' 
     declarations   
  ;
  
declarations
  : partiedef? partieref? consts? vars? decprocs? 
  ;
  
partiedef
  : 'def' ident  (',' ident )* ptvg
  ;
  
partieref: 'ref'  specif (',' specif)* ptvg
  ;
  
specif  : ident  ( 'fixe' '(' type  ( ',' type  )* ')' )? 
                 ( 'mod'  '(' type  ( ',' type  )* ')' )? 
  ;
  
consts  : 'const' ( ident '=' valeur {PtGen.pt(7);} ptvg  )+ 
  ;
  
vars  : 'var' ( type ident {PtGen.pt(26);} ( ','  ident {PtGen.pt(26);} )* ptvg )+ {PtGen.pt(6);}
  ;
  
type  : 'ent' {PtGen.pt(10);}
  |     'bool' {PtGen.pt(9);}
  ;
  
decprocs: (decproc ptvg)+
  ;
  
decproc :  'proc'  ident  parfixe? parmod? consts? vars? corps 
  ;
  
ptvg  : ';'
  | 
  ;
  
corps : 'debut' instructions 'fin' {PtGen.pt(255);}
  ;
  
parfixe: 'fixe' '(' pf ( ';' pf)* ')'
  ;
  
pf  : type ident  ( ',' ident  )*  
  ;

parmod  : 'mod' '(' pm ( ';' pm)* ')'
  ;
  
pm  : type ident  ( ',' ident  )*
  ;
  
instructions
  : instruction ( ';' instruction)*
  ;
  
instruction
  : inssi
  | inscond
  | boucle
  | lecture
  | ecriture
  | affouappel
  |
  ;
  
inssi : 'si' expression 'alors' instructions ('sinon'  instructions)? 'fsi' 
  ;
  
inscond : 'cond'  expression  ':' instructions 
          (','  expression  ':' instructions )* 
          ('aut'  instructions)? 'fcond' 
  ;
  
boucle  : 'ttq'  expression 'faire' instructions 'fait' 
  ;
  
lecture: 'lire' '(' ident  ( ',' ident  )* ')' 
  ;
  
ecriture: 'ecrire' '(' expression ( ',' expression  )* ')' {PtGen.pt(13);}
   ;
  
affouappel
  : ident  (    ':=' expression 
            |   (effixes (effmods)?)?  
           )
  ;
  
effixes : '(' (expression {PtGen.pt(5);} (',' expression  )*)? ')'
  ;
  
effmods :'(' (ident  (',' ident  )*)? ')'
  ; 
  
expression: (exp1) ('ou'  exp1 {PtGen.pt(24);}  )*
  ;
  
exp1  : exp2 ('et'  exp2 {PtGen.pt(23);} )* 
  ;
  
exp2  : 'non' exp2 {PtGen.pt(22);}
  | exp3  
  ;
  
exp3  : exp4 
  ( '='   exp4 {PtGen.pt(16);}
  | '<>'  exp4 {PtGen.pt(17);}
  | '>'   exp4 {PtGen.pt(18);}
  | '>='  exp4 {PtGen.pt(19);}
  | '<'   exp4 {PtGen.pt(20);}
  | '<='  exp4 {PtGen.pt(21);}
  ) ?
  ;
  
exp4  : exp5 
        ('+'  exp5 {PtGen.pt(5);}
        |'-'  exp5 {PtGen.pt(4);}
        )*
  ;
  
exp5  : primaire 
        (    '*'   primaire {PtGen.pt(14);}
          | 'div'  primaire {PtGen.pt(15);}
        )*
  ;
  
primaire: valeur {PtGen.pt(3);}
  | ident {PtGen.pt(26);}
  | '(' expression ')'
  ;
  
valeur  : nbentier {PtGen.pt(27);}
  | '+' {PtGen.pt(11);} nbentier {PtGen.pt(27);}
  | '-' {PtGen.pt(11);} nbentier {PtGen.pt(25);}
  | 'vrai' {PtGen.pt(2);}
  | 'faux' {PtGen.pt(1);}
  ;

// partie lexicale  : cette partie ne doit pas �tre modifi�e  //
// les unit�s lexicales de ANTLR doivent commencer par une majuscule
// attention : ANTLR n'autorise pas certains traitements sur les unit�s lexicales, 
// il est alors n�cessaire de passer par un non-terminal interm�diaire 
// exemple : pour l'unit� lexicale INT, le non-terminal nbentier a d� �tre introduit
 
      
nbentier  :   INT { UtilLex.valNb = Integer.parseInt($INT.text);}; // mise � jour de valNb

ident : ID  { UtilLex.traiterId($ID.text, $ID.line); } ; // mise � jour de numId
     // tous les identificateurs seront plac�s dans la table des identificateurs, y compris le nom du programme ou module
     // la table des symboles n'est pas g�r�e au niveau lexical
        
  
ID  :   ('a'..'z'|'A'..'Z')('a'..'z'|'A'..'Z'|'0'..'9'|'_')* ; 
     
// zone purement lexicale //

INT :   '0'..'9'+ ;
WS  :   (' '|'\t' | '\n' |'\r')+ {skip();} ; // d�finition des "espaces"


COMMENT
  :  '\{' (.)* '\}' {skip();}   // toute suite de caract�res entour�e d'accolades est un commentaire
  |  '#' ~( '\r' | '\n' )* {skip();}  // tout ce qui suit un caract�re di�se sur une ligne est un commentaire
  ;

// commentaires sur plusieurs lignes
ML_COMMENT    :   '/*' (options {greedy=false;} : .)* '*/' {$channel=HIDDEN;}
    ;	   



	   