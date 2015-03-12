/*********************************************************************************
 *       VARIABLES ET METHODES FOURNIES PAR LA CLASSE UtilLex.java               *
 *       compl�ment � l'ANALYSEUR LEXICAL produit par ANTLR                      *
 *                                                                               *
 *                                                                               *
 *   nom du programme compil�, sans suffixe : String UtilLex.nomSource           *
 *   ------------------------                                                    *
 *                                                                               *
 *   attributs lexicaux (selon items figurant dans la grammaire):                *
 *   ------------------                                                          *
 *     int UtilLex.valNb = valeur du dernier nombre entier lu (item nbentier)    *
 *     int UtilLex.numId = code du dernier identificateur lu (item ident)        *
 *                                                                               *
 *                                                                               *
 *   m�thodes utiles :                                                           *
 *   ---------------                                                             *
 *     void UtilLex.messErr(String m)  affichage de m et arr�t compilation       *
 *     String UtilLex.repId(int nId) d�livre l'ident de codage nId               *
 *     
 *********************************************************************************     
 *     METHODES FOURNIES PAR LA CLASSE PtGen.java                                *
 *     constGen() et constObj()  fournissent les deux fichiers objets            *  
 *     void afftabSymb()  affiche la table des symboles                          *
 *********************************************************************************/

// NB: Merci de renseigner la variable (String) trinome, d�finie plus loin

import java.io.*;

public class PtGen {

	// constantes manipul�es par le compilateur
	// ----------------------------------------

	private static final int

	MAXSYMB = 300,
			MAXOBJ = 1000,

			// codes MAPILE :
			RESERVER = 1, EMPILER = 2, CONTENUG = 3, AFFECTERG = 4, OU = 5,
			ET = 6, NON = 7, INF = 8, INFEG = 9, SUP = 10, SUPEG = 11, EG = 12,
			DIFF = 13, ADD = 14, SOUS = 15, MUL = 16, DIV = 17, BSIFAUX = 18,
			BINCOND = 19, LIRENT = 20, LIREBOOL = 21, ECRENT = 22,
			ECRBOOL = 23, ARRET = 24, EMPILERADG = 25, EMPILERADL = 26,
			CONTENUL = 27, AFFECTERL = 28, APPEL = 29, RETOUR = 30,

			// types permis :
			ENT = 1, BOOL = 2, NEUTRE = 3,

			// cat�gories possibles :
			CONSTANTE = 1, VARGLOBALE = 2, VARLOCALE = 3, PARAMFIXE = 4,
			PARAMMOD = 5, PROC = 6, DEF = 7, REF = 8, PRIVEE = 9;

	// table des symboles
	// ------------------

	private static class EltTabSymb {
		public int code, categorie, type, info;

		public EltTabSymb() {
		}

		public EltTabSymb(int code, int categorie, int type, int info) {
			this.code = code;
			this.categorie = categorie;
			this.type = type;
			this.info = info;
		}

		public String toString() {
			final String[] chcat = { "", "CONSTANTE      ", "VARGLOBALE     ",
					"VARLOCALE      ", "PARAMFIXE      ", "PARAMMOD       ",
					"PROC           ", "DEF            ", "REF            ",
					"PRIVEE         " };
			final String[] chtype = { "", "ENT     ", "BOOL    ", "NEUTRE  " };
			String ch = "";
			if (code == -1)
				ch += "-1";
			else
				ch += "@" + UtilLex.repId(code);
			while (ch.length() < 15)
				ch += ' ';
			return ch + chcat[categorie] + chtype[type] + info;
		} // toString
	} // EltTabSymb

	private static EltTabSymb[] tabSymb = new EltTabSymb[MAXSYMB + 1];
	private static int it, bc;

	private static int presentIdent(int binf) {
		int i = it;
		while (i >= binf && tabSymb[i].code != UtilLex.numId)
			i--;
		if (i >= binf)
			return i;
		else
			return 0;
	}

	private static void placeIdent(int c, int cat, int t, int v) {
		if (it == MAXSYMB)
			UtilLex.messErr("d�bordement de la table des symboles");
		it = it + 1;
		tabSymb[it] = new EltTabSymb(c, cat, t, v);
	}

	private static void afftabSymb() { // affiche la table des symboles
		System.out.println("       code           categorie      type    info");
		System.out.println("      |--------------|--------------|-------|----");
		for (int i = 1; i <= it; i++) {
			if (i == bc) {
				System.out.print("bc=");
				Ecriture.ecrireInt(i, 3);
			} else if (i == it) {
				System.out.print("it=");
				Ecriture.ecrireInt(i, 3);
			} else
				Ecriture.ecrireInt(i, 6);
			if (tabSymb[i] == null)
				System.out.println(" r�f�rence NULL");
			else
				System.out.println(" " + tabSymb[i]);
		}
		System.out.println();
	}

	// contr�le de type
	// ----------------

	private static void verifEnt() {
		if (tCour != ENT)
			UtilLex.messErr("expression enti�re attendue");
	}

	private static void verifBool() {
		if (tCour != BOOL)
			UtilLex.messErr("expression bool�enne attendue");
	}

	// pile pour g�rer les cha�nes de reprise et les branchements en avant
	// -------------------------------------------------------------------

	private static class TpileRep { // chaines de reprise it�rations,
									// conditionnelles
		private final int MAXPILEREP = 50;
		private int ip;
		private int[] T = new int[MAXPILEREP + 1];

		public void empiler(int x) {
			if (ip == MAXPILEREP)
				UtilLex.messErr("d�bordement de la pile de gestion des reprises");
			ip = ip + 1;
			T[ip] = x;
		}

		public int depiler() {
			if (ip == 0)
				UtilLex.messErr("compilateur en croix sur chaine de reprise ");
			ip = ip - 1;
			return T[ip + 1];
		}

		public TpileRep() {
			ip = 0;
		}
	} // TpileRep

	private static TpileRep pileRep = new TpileRep();; // chaines de reprise
														// it�rations,
														// conditionnelles

	// production du code objet en m�moire, dans le tableau po
	// -------------------------------------------------------

	private static int[] po = new int[MAXOBJ + 1];
	private static int ipo;

	private static void produire(int codeouarg) {
		if (ipo == MAXOBJ)
			UtilLex.messErr("d�bordement : programme objet trop long");
		ipo = ipo + 1;
		po[ipo] = codeouarg;
	}

	// construction du fichier objet sous forme mn�monique
	// ---------------------------------------------------
	private static void constGen() {
		Mnemo.creerFichier(ipo, po, UtilLex.nomSource + ".gen"); // recopie de
																	// po sous
																	// forme
																	// mn�monique
	}

	// construction du fichier objet pour MAPILE
	// -----------------------------------------
	private static void constObj() {
		OutputStream f = Ecriture.ouvrir(UtilLex.nomSource + ".obj");
		if (f == null) {
			System.out.println("impossible de cr�er " + UtilLex.nomSource
					+ ".obj");
			System.exit(1);
		}
		for (int i = 1; i <= ipo; i++)
			if (vTrans[i] != -1)
				Ecriture.ecrireStringln(f, i + "   " + vTrans[i]);
		for (int i = 1; i <= ipo; i++)
			Ecriture.ecrireStringln(f, "" + po[i]);
		Ecriture.fermer(f);
	}

	// autres variables et proc�dures fournies
	// ---------------------------------------
	public static String trinome = "ATTARD DOUX RADENAC"; // RENSEIGNER ICI LES NOMS DU
													// TRINOME, constitu�s
													// exclusivement de lettres

	private static int tCour; // type de l'expression compil�e
	private static int vCour; // valeur de l'expression compil�e le cas echeant
	

	// compilation s�par�e : vecteur de translation et descripteur
	// -----------------------------------------------------------

	private static int[] vTrans = new int[MAXOBJ + 1];

	private static void initvTrans() {
		for (int i = 1; i <= MAXOBJ; i++)
			vTrans[i] = -1;
	}

	private static Descripteur desc;

	private static void vecteurTrans(int x) { // ajout d'un doublet au vecteur
												// de translation
		if (x == Descripteur.REFEXT || desc.unite.equals("module")) {
			vTrans[ipo] = x;
			desc.nbTransExt++;
		}
	} // descripteur

	// initialisations � compl�ter
	// -----------------------------

	private static void initialisations() { // � compl�ter si n�cessaire mais NE
											// RIEN SUPPRIMER
		initvTrans();
		desc = new Descripteur(); // initialisation du descripteur pour
									// compilation s�par�e
		it = 0;
		bc = 1;
		ipo = 0;
		tCour = NEUTRE;
		tCour = NEUTRE;
		nbvar = 0; // indice de placement des varglobales
		x = 0;
		aAffecter=0;
		aAffecter=0;
		nbvarproc=0; //param fixe et mod
		nbvarident=0; //variable locale
		pourAppelEFixe=0; // permet de voir cb il y a de effixes
		pourAppelEMod=0; // permet de voir cb il y a de efmods
		
		
	} // initialisations
	
	// autres variables et proc�dures introduites par le trinome
	private static int nbvar =0;
	private static int x=0;
	private static int nbvarproc=0;
	private static int nbvarident=0;
	private static int aAffecter=0;
	private static int aAffecter2=0;
	private static int pourAppelEMod=0;
	private static int pourAppelEFixe=0;
	// code des points de g�n�ration � compl�ter
	// -----------------------------------------
	public static void pt(int numGen) {
		int cat;
		switch (numGen) {
		case 0:
			initialisations();
			break;
		// faux
		case 1:
			tCour = BOOL;
			vCour = 0;
			break;
		// vrai
		case 2:
			tCour = BOOL;
			vCour = 1;
			break;
		// empiler la valeur
		case 3:
			produire(EMPILER);
			produire(vCour);
			break;
		// nbentier positif
		case 4:
			tCour = ENT;
			vCour = UtilLex.valNb;
			break;
		// nbentier n�gatif
		case 5:
			tCour = ENT;
			vCour = -UtilLex.valNb;
			break;
		// -
		case 6:
			produire(SOUS);
			break;
		// +
		case 7:
			produire(ADD);
			break;
		// code Mapile reserver
		case 8:
			if(bc==1){
			produire(RESERVER);
			produire(nbvar); // Nb de variables � sauvegarder
			}
			else{
				if(nbvarident!=0){
				produire(RESERVER);
				produire(nbvarident);
				}
			}
			break;
		// declaration consts
		case 9:
			if (presentIdent(1) == 0) {
				placeIdent(UtilLex.numId, CONSTANTE, tCour, vCour);
			}
			break;
		// Declaration de varglobale ou locale
		case 10:
			if (presentIdent(1) == 0) {
				if (bc ==1){
					placeIdent(UtilLex.numId, VARGLOBALE, tCour, nbvar);
					nbvar++;
				}
				else{
					placeIdent(UtilLex.numId, VARLOCALE, tCour,nbvarproc+nbvarident+2);
					nbvarident++;
				}
			}
			break;
		// type bool
		case 11:
			tCour = BOOL;
			break;
		// type ent
		case 12:
			tCour = ENT;
			break;
		// test entier
		case 13:
			verifEnt();
			break;
		// test bool
		case 14:
			verifBool();
			break;
		// ecriture
		case 15:
			if (tCour == ENT)
				produire(ECRENT);
			else if (tCour == BOOL)
				produire(ECRBOOL);
			break;
		case 16:
			produire(MUL);
			tCour = ENT;
			break;
		// /
		case 17:
			produire(DIV);
			tCour = ENT;
			break;
		// traitement des expressions 3
		case 18:
			produire(EG);
			tCour = BOOL;
			break;
		case 19:
			produire(DIFF);
			tCour = BOOL;
			break;
		case 20:
			produire(SUP);
			tCour = BOOL;
			break;
		case 21:
			produire(SUPEG);
			tCour = BOOL;
			break;
		case 22:
			produire(INF);
			tCour = BOOL;
			break;
		case 23:
			produire(INFEG);
			tCour = BOOL;
			break;
		// NON
		case 24:
			produire(NON);
			tCour = BOOL;
			break;
		// ET
		case 25:
			produire(ET);
			tCour = BOOL;
			break;
		// OU
		case 26:
			produire(OU);
			tCour = BOOL;
			break;
		// Maj table des symboles
		case 27:
			x = presentIdent(1);
			if (x == 0)
				UtilLex.messErr("Maj tabSymb : identificateur non d�clar�");
			tCour = tabSymb[x].type;
			switch (tabSymb[x].categorie) {
			case CONSTANTE:
				produire(EMPILER);
				produire(tabSymb[x].info);
				break;
			case VARGLOBALE:
				produire(CONTENUG);
				produire(tabSymb[x].info);
				break;
			case PARAMFIXE:
				produire(CONTENUL);
				produire(tabSymb[x].info);
				produire(0);
				break;
			case VARLOCALE:
				produire(CONTENUL);
				produire(tabSymb[x].info);
				produire(0);
				break;
			case PARAMMOD:
				produire(CONTENUL);
				produire(tabSymb[x].info);
				produire(1);
				break;
			default:
				UtilLex.messErr("Maj tabSymb : cas non pris en compte");
				break;
			}
			break;
		// Affectation de variable globale ou locale
		case 28:
			if(bc==1){
			produire(AFFECTERG);
			produire(aAffecter);
			}
			else{
				produire(AFFECTERL);
				produire(aAffecter);
				switch (tabSymb[x].categorie) {
				case VARLOCALE:
					produire(0);
					break;
				case PARAMMOD:
					produire(1);
					break;
				default:
					UtilLex.messErr("Maj tabSymb : cas non pris en compte");
					break;
				}
			}
			break;
		// Teste si c'est une variable � affecter et l'enregistre
		case 29:
			x = presentIdent(1);
			if (x == 0)
				UtilLex.messErr("Enregistrement variable : identificateur non d�clar�");
			switch (tabSymb[x].categorie) {
			case PROC:
				aAffecter=tabSymb[x].info;
				aAffecter2=tabSymb[x+1].info;
				break;
			case CONSTANTE:
				UtilLex.messErr("Variable globale/locale/parammod attendue");
				break;
			case VARGLOBALE:
				aAffecter = tabSymb[x].info;
				break;
			case VARLOCALE:
				aAffecter = tabSymb[x].info;
				break;
			case PARAMFIXE:
				UtilLex.messErr("Variable globale/locale/parammod attendue");
				break;
			case PARAMMOD:
				aAffecter = tabSymb[x].info;
				break;
			default:
				UtilLex.messErr("Enregistrement variable : cas non pris en compte");
				break;
			}
			break;
		// Lecture
		case 30:
			x = presentIdent(1);
			if (x == 0)
				UtilLex.messErr("Lecture : identificateur non d�clar�");
			switch (tabSymb[x].type) {
			case BOOL:
				produire(LIREBOOL);
				break;
			case ENT:
				produire(LIRENT);
				break;
			default:
				UtilLex.messErr("Lecture : cas non pris en compte");
				break;
			}
			if(bc==1){
			produire(AFFECTERG);
			produire(tabSymb[x].info);
			}
			else{
				produire(AFFECTERL);
				produire(tabSymb[x].info);
				switch (tabSymb[x].categorie) {
				case VARLOCALE:
					produire(0);
					break;
				case PARAMMOD:
					produire(1);
					break;
				}
			}
			break;
		// Si OU condition d'arr�t de ttq OU cond
		case 31:
			verifBool();
			produire(BSIFAUX);
			produire(0);
			pileRep.empiler(ipo);
			break;
		// Alors
		case 32:
			produire(BINCOND);
			produire(po[ipo]);
			po[pileRep.depiler()] = ipo + 1;
			pileRep.empiler(ipo);
			break;
		// fin si
		case 33:
			po[pileRep.depiler()] = ipo + 1;
			break;
		// ttq
		case 34:
			pileRep.empiler(ipo + 1);
			break;
		// sortie de boucle
		case 35:
			produire(BINCOND);
			po[pileRep.depiler()] = ipo + 2;
			produire(pileRep.depiler());
			break;
		// Debut cond : chaine de reprise vide
		case 36:
			pileRep.empiler(0);
			break;
		//autre
		case 37 :
			produire(BINCOND);
			produire(0);
			po[pileRep.depiler()] = ipo + 1;
			pileRep.empiler(ipo);
			break;
		// apr�s 1er bincond
		case 38:
			produire(BINCOND);
			po[pileRep.depiler()] = ipo + 2;
			produire(pileRep.depiler());
			pileRep.empiler(ipo);
			break;
		//Fin du cond, remonte la cha�ne de reprise
		case 39:
			po[pileRep.depiler()] = ipo + 1;
			int valdep = pileRep.depiler();//Besoin d'une variable pour ne pas d�piler deux fois en faisant po[pileRep.depiler()]
			while(valdep != 0){
				pileRep.empiler(po[valdep]);//Empile la valeur du bincond cha�n�
				po[valdep] = ipo + 1;
				valdep = pileRep.depiler();
			}
			break;
			
		//maj tabsymb pour proc�dure
		case 41:
			if (presentIdent(1) == 0) {
				placeIdent(UtilLex.numId, PROC, NEUTRE, ipo+1);
				placeIdent(-1,PRIVEE,NEUTRE, 0);
				bc=it+1;
				nbvarproc=0;
			}
			break;
		//paramfixe	
		case 42:
			if (presentIdent(1) == 0) {
				placeIdent(UtilLex.numId, PARAMFIXE, tCour, nbvarproc);
				nbvarproc++;
			}
			
			break;
		//Parammod
		case 43:
			if (presentIdent(1) == 0) {
				placeIdent(UtilLex.numId, PARAMMOD, tCour, nbvarproc);
				nbvarproc++;
			}
			
			break;
		//Nb de param pour la proc�dure courante
		case 44:
			tabSymb[bc-1].info=it-bc+1;
			break;
		//bincond de proc�dure
		case 45:
			produire(BINCOND);
			produire(po[ipo]);
			pileRep.empiler(ipo);
			break;
		//Appel de proc�dure
		case 46:
			if((pourAppelEFixe + pourAppelEMod)==aAffecter2){
				produire(APPEL);
				produire(aAffecter);
				produire(aAffecter2);
			}
			else {
				UtilLex.messErr("Il n'y a pas assez de param�tre dans l'appel");
			}
			break;
		//param mod � l'appel	
		case 47:
			x=presentIdent(1);
			if(x==0)
				UtilLex.messErr("Lecture : identificateur non d�clar�");
			switch (tabSymb[x].categorie) {
			case CONSTANTE:
				UtilLex.messErr("Variable globale/locale/parammod attendue");
				break;
			case VARGLOBALE:
				produire(EMPILERADG);
				produire(tabSymb[x].info);
				break;
		case VARLOCALE:
				produire(EMPILERADL);
				produire(tabSymb[x].info);
				produire(0);
				break;
			case PARAMFIXE:
				UtilLex.messErr("Variable globale/locale/parammod attendue");
				break;
			case PARAMMOD:
				produire(EMPILERADL);
				produire(tabSymb[x].info);
				produire(1);
				break;
			}
			break;
		//Compte le nb de param fixes � l'appel
		case 48:
			pourAppelEFixe++;
			break;
		//Compte le nb de param mod � l'appel	
		case 49:
			pourAppelEMod++;
			break;
		
		case 255:
			if(bc==1){
			produire(ARRET);
			constGen();
			constObj();
			afftabSymb();
			}
			else{
				it=it-nbvarident;
				for(int i= bc; i<=it;++i){
					tabSymb[i].code=-1;
				}
				produire(RETOUR);
				produire(tabSymb[bc-1].info);
				po[pileRep.depiler()] = ipo + 1;
				bc=1;
				nbvarident=0;
				nbvarproc=0;
			}
			break;

		default:
			System.out.println("Point de g�n�ration non pr�vu dans votre liste");
			break;

		}
	}
}
