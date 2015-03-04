/*********************************************************************************
 *       VARIABLES ET METHODES FOURNIES PAR LA CLASSE UtilLex.java               *
 *       complément à l'ANALYSEUR LEXICAL produit par ANTLR                      *
 *                                                                               *
 *                                                                               *
 *   nom du programme compilé, sans suffixe : String UtilLex.nomSource           *
 *   ------------------------                                                    *
 *                                                                               *
 *   attributs lexicaux (selon items figurant dans la grammaire):                *
 *   ------------------                                                          *
 *     int UtilLex.valNb = valeur du dernier nombre entier lu (item nbentier)    *
 *     int UtilLex.numId = code du dernier identificateur lu (item ident)        *
 *                                                                               *
 *                                                                               *
 *   méthodes utiles :                                                           *
 *   ---------------                                                             *
 *     void UtilLex.messErr(String m)  affichage de m et arrêt compilation       *
 *     String UtilLex.repId(int nId) délivre l'ident de codage nId               *
 *     
 *********************************************************************************     
 *     METHODES FOURNIES PAR LA CLASSE PtGen.java                                *
 *     constGen() et constObj()  fournissent les deux fichiers objets            *  
 *     void afftabSymb()  affiche la table des symboles                          *
 *********************************************************************************/

// NB: Merci de renseigner la variable (String) trinome, définie plus loin

import java.io.*;

public class PtGen {

	// constantes manipulées par le compilateur
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

			// catégories possibles :
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
			UtilLex.messErr("débordement de la table des symboles");
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
				System.out.println(" référence NULL");
			else
				System.out.println(" " + tabSymb[i]);
		}
		System.out.println();
	}

	// contrôle de type
	// ----------------

	private static void verifEnt() {
		if (tCour != ENT)
			UtilLex.messErr("expression entière attendue");
	}

	private static void verifBool() {
		if (tCour != BOOL)
			UtilLex.messErr("expression booléenne attendue");
	}

	// pile pour gérer les chaînes de reprise et les branchements en avant
	// -------------------------------------------------------------------

	private static class TpileRep { // chaines de reprise itérations,
									// conditionnelles
		private final int MAXPILEREP = 50;
		private int ip;
		private int[] T = new int[MAXPILEREP + 1];

		public void empiler(int x) {
			if (ip == MAXPILEREP)
				UtilLex.messErr("débordement de la pile de gestion des reprises");
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
														// itérations,
														// conditionnelles

	// production du code objet en mémoire, dans le tableau po
	// -------------------------------------------------------

	private static int[] po = new int[MAXOBJ + 1];
	private static int ipo;

	private static void produire(int codeouarg) {
		if (ipo == MAXOBJ)
			UtilLex.messErr("débordement : programme objet trop long");
		ipo = ipo + 1;
		po[ipo] = codeouarg;
	}

	// construction du fichier objet sous forme mnémonique
	// ---------------------------------------------------
	private static void constGen() {
		Mnemo.creerFichier(ipo, po, UtilLex.nomSource + ".gen"); // recopie de
																	// po sous
																	// forme
																	// mnémonique
	}

	// construction du fichier objet pour MAPILE
	// -----------------------------------------
	private static void constObj() {
		OutputStream f = Ecriture.ouvrir(UtilLex.nomSource + ".obj");
		if (f == null) {
			System.out.println("impossible de créer " + UtilLex.nomSource
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

	// autres variables et procédures fournies
	// ---------------------------------------
	public static String trinome = "ATTARD DOUX RADENAC"; // RENSEIGNER ICI LES
															// NOMS DU
	// TRINOME, constitués
	// exclusivement de lettres

	private static int tCour; // type de l'expression compilée
	private static int vCour; // valeur de l'expression compilée le cas echeant
	private static int bp;
	private static int y;
	private static int x;
	private static int aAffecter; // Variable a affecter

	// compilation séparée : vecteur de translation et descripteur
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

	// initialisations à compléter
	// -----------------------------

	private static void initialisations() { // à compléter si nécessaire mais NE
											// RIEN SUPPRIMER
		initvTrans();
		desc = new Descripteur(); // initialisation du descripteur pour
									// compilation séparée
		it = 0;
		bc = 1;
		ipo = 0;
		tCour = NEUTRE;
		bp = 0;
		y = 0; // indice de placement des varglobales
		x = 0;
	} // initialisations

	// autres variables et procédures introduites par le trinome

	// code des points de génération à compléter
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
		// nbentier négatif
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
			produire(RESERVER);
			produire(bp); // Nb de variables à sauvegarder
			break;
		// declaration consts
		case 9:
			// y = UtilLex.numId;
			if (presentIdent(1) == 0) {
				placeIdent(UtilLex.numId, CONSTANTE, tCour, vCour);
			}
			afftabSymb();
			break;
		// Declaration de varglibale
		case 10:
			if (presentIdent(1) == 0) {
				placeIdent(UtilLex.numId, VARGLOBALE, tCour, bp);
				bp++;
			}
			afftabSymb();
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
			break;
		// /
		case 17:
			produire(DIV);
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
			break;
		// ET
		case 25:
			produire(ET);
			break;
		// OU
		case 26:
			produire(OU);
			break;
		// Maj table des symboles
		case 27:
			x = presentIdent(1);
			if (x == 0)
				UtilLex.messErr("Maj tabSymb : identificateur non déclaré");
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
			default:
				UtilLex.messErr("Maj tabSymb : cas non pris en compte");
				break;
			}
			afftabSymb();
			break;
		// Affectation de variable globale
		case 28:
			produire(AFFECTERG);
			produire(aAffecter);
			break;
		// Teste si c'est une variable à affecter et l'enregistre
		case 29:
			x = presentIdent(1);
			if (x == 0)
				UtilLex.messErr("Enregistrement variable : identificateur non déclaré");
			switch (tabSymb[x].categorie) {
			case CONSTANTE:
				UtilLex.messErr("Variable globale attendue");
				break;
			case VARGLOBALE:
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
				UtilLex.messErr("Lecture : identificateur non déclaré");
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
			produire(AFFECTERG);
			produire(tabSymb[x].info);
			break;
		// Si OU condition d'arrêt de ttq OU cond
		case 31:
			verifBool();
			produire(BSIFAUX);
			produire(po[ipo]);
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
			System.out.println("apres ttq ipo " + ipo);
			break;
		// sortie de boucle
		case 36:
			produire(BINCOND);
			po[pileRep.depiler()] = ipo + 2;
			produire(pileRep.depiler());
			break;
		// Debut cond : chaine de reprise vide
		case 37:
			pileRep.empiler(0);
			break;
		// après 1er bincond
		case 39:
			produire(BINCOND);
			produire(po[ipo]);
			po[pileRep.depiler()] = ipo + 1;
			pileRep.empiler(ipo);
			break;
		//Fin du cond, remonte la chaîne de reprise
		case 40:
			while(pileRep.depiler() != 0){
				po[pileRep.depiler()] = ipo;
			}
			break;
		case 255:
			produire(ARRET);
			constGen();
			constObj();
			break;

		// traitement du cond

		// etc

		default:
			System.out
					.println("Point de génération non prévu dans votre liste");
			break;

		}
	}
}
