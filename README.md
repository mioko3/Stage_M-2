# Manuel d'utilisation — Planning Global Futura

> Ce document explique comment utiliser l'application **Planning Global Futura** pas à pas.

---

## Sommaire

1. [Lancer l'application](#1-lancer-lapplication)
2. [Comprendre l'écran principal](#2-comprendre-lécran-principal)
3. [Importer des lots depuis Excel](#3-importer-des-lots-depuis-excel)
4. [Onglet Affectation — attribuer un lot à une société](#4-onglet-affectation--attribuer-un-lot-à-une-société)
5. [Onglet Sociétés & heures — gérer les sociétés](#5-onglet-sociétés--heures--gérer-les-sociétés)
6. [Onglet Liste des lots — consulter et modifier les lots](#6-onglet-liste-des-lots--consulter-et-modifier-les-lots)
7. [Onglet Fiches de Route — voir le détail par société](#7-onglet-fiches-de-route--voir-le-détail-par-société)
8. [Sauvegarder et charger les données](#8-sauvegarder-et-charger-les-données)
9. [Remettre à zéro (nouvelle semaine)](#9-remettre-à-zéro-nouvelle-semaine)
10. [Questions fréquentes](#10-questions-fréquentes)
11. [Idées](#11-idées)

---

## 1. Lancer l'application

1. Ouvrez le dossier du projet.
2. **Double-cliquez sur le fichier `run.bat`** (sous Windows).
3. L'application s'ouvre automatiquement. Aucune installation n'est nécessaire.

> ⚠️ Si une fenêtre noire (invite de commandes) s'ouvre brièvement, c'est normal. Elle se ferme seule.

---

## 2. Comprendre l'écran principal

L'écran se divise en deux zones :

- **En haut** : une barre d'information qui affiche en temps réel le nombre de lots, de sociétés, le nombre de lots affectés et les heures disponibles au total.
- **Au centre** : quatre onglets permettent d'accéder aux différentes fonctions.

| Onglet | À quoi ça sert |
|---|---|
| ⊕ Affectation | Attribuer des lots à des sociétés |
| ▤ Sociétés & heures | Voir et modifier les sociétés et leurs heures disponibles |
| ☰ Liste des lots | Consulter, filtrer, modifier ou supprimer des lots |
| 📋 Fiches de Route | Voir la fiche détaillée de chaque société |

En haut à gauche, le menu **Fichier** permet de sauvegarder, charger ou réinitialiser les données.

---

## 3. Importer des lots depuis Excel

L'application lit automatiquement un fichier Excel fourni (le fichier planning hebdomadaire). Voici comment l'utiliser :

1. Dans le menu **Fichier**, l'application charge les données du fichier `export.xlsx` situé dans le dossier `app/data/` qu'il faut rentré au tout début.
2. Si vous avez un nouveau fichier Excel à charger, **remplacez le fichier `export.xlsx`** dans ce même dossier par votre nouveau fichier, en conservant le même nom.
3. Relancez l'application : les lots seront automatiquement importés.

> 💡 **Les heures ne se calculent pas encore automatiquement.** Des valeurs par défaut ont été pré-remplies. Si les heures d'un lot sont incorrectes, vous pouvez les modifier manuellement (voir section 6).

---

## 4. Onglet Affectation — attribuer un lot à une société

Cet onglet est le cœur de l'application. Il est divisé en trois colonnes :

- **Gauche** : liste de tous les lots disponibles (non encore affectés)
- **Centre** : détail du lot sélectionné + formulaire d'affectation
- **Droite** : liste des lots déjà affectés

### Comment affecter un lot à une société

1. Cliquez sur un lot dans la **colonne de gauche** pour le sélectionner.
2. Ses informations apparaissent dans la **colonne centrale**.
3. Dans la colonne centrale, choisissez une **société** dans la liste déroulante.
4. Choisissez ensuite un **ACE** (équipe/groupe) dans la deuxième liste déroulante.
5. Cliquez sur le bouton **▶ Affecter →**.
6. Le lot disparaît de la colonne gauche et apparaît dans la colonne droite.

### Comment retirer une affectation

1. Cliquez sur un lot dans la **colonne de droite** (lots affectés).
2. Cliquez sur le bouton **◀ Retirer**.
3. Le lot revient dans la colonne de gauche.

### Autres boutons disponibles

- **✏ Modifier ce lot** : ouvre une fenêtre pour changer les informations du lot sélectionné.
- **+ Nouveau lot** : permet de créer manuellement un nouveau lot.

> 🔍 Vous pouvez utiliser la **barre de recherche** au-dessus de chaque tableau pour filtrer les lots par numéro de commande, type ou affaire.

---

## 5. Onglet Sociétés & heures — gérer les sociétés

Cet onglet affiche un tableau récapitulatif de toutes les sociétés avec leurs informations.

| Colonne | Signification |
|---|---|
| Société | Nom de la société |
| CE | Nom du responsable CE |
| H initiales | Nombre d'heures attribuées au départ |
| H restantes | Heures encore disponibles (coloré en vert/orange/rouge selon le niveau) |
| % consommé | Pourcentage des heures déjà utilisées |
| Lots | Nombre de lots affectés à cette société |
| ACE | Nombre d'équipes (ACE) dans cette société |

### Modifier une société

1. Cliquez sur une ligne pour sélectionner une société.
2. Cliquez sur **✏ Modifier la société sélectionnée** (ou double-cliquez sur la ligne).
3. Une fenêtre s'ouvre : vous pouvez changer le nom, le CE, et le total d'heures.
4. Cliquez sur **Enregistrer** pour valider.

### Mettre à jour les heures en début de semaine

Cliquez sur le bouton **Nouvelle heure** pour recalculer automatiquement les heures restantes en fonction du numéro de semaine.

---

## 6. Onglet Liste des lots — consulter et modifier les lots

Cet onglet affiche **tous les lots** enregistrés dans l'application.

### Filtrer et rechercher

En haut du tableau, vous disposez de trois outils de filtrage :

- **Filtre Statut** : affiche uniquement les lots d'un certain statut (`VA - Validé`, `BL - Bloqué`, `EP - Envoi au CP`).
- **Case "Inclure les lots sous douane"** : les lots sous douane sont cachés par défaut (affichés en rouge quand la case est cochée).
- **Champ Recherche** : tapez un numéro de commande, une typologie ou une affaire pour filtrer instantanément.

### Modifier un lot

1. Cliquez sur un lot pour le sélectionner.
2. **Double-cliquez** dessus (ou cliquez sur **✏ Modifier**).
3. Une fenêtre s'ouvre avec tous les champs modifiables : typography, nombre de pièces, cadence, statut, semaine, emplacement, commentaire, etc.
4. Cliquez sur **Enregistrer** pour valider.

### Supprimer un lot

1. Sélectionnez un lot **non affecté** (un lot affecté ne peut pas être supprimé directement).
2. Cliquez sur **🗑 Supprimer**.
3. Confirmez la suppression dans la fenêtre qui apparaît.

> ⚠️ Pour supprimer un lot affecté, vous devez d'abord **retirer son affectation** dans l'onglet Affectation.

---

## 7. Onglet Fiches de Route — voir le détail par société

Cet onglet génère automatiquement une **fiche de route** pour chaque société.

### Comment l'utiliser

1. Sélectionnez une société dans la liste déroulante en haut.
2. La fiche s'affiche avec :
   - **Un résumé global** : valeur totale, nombre de pièces, heures disponibles.
   - **Un résumé par ACE** : détail par équipe.
   - **Un tableau détaillé** des lots, regroupés par ACE, avec les colonnes de suivi de production (étiquetage, répartition, avancement, heures restantes, etc.).

### Mettre à jour le suivi de production

Dans le tableau de la fiche de route, vous pouvez directement saisir :
- Le **nombre de pièces étiquetées**
- Le **nombre de pièces réparties**

Ces valeurs sont sauvegardées automatiquement.

---

## 8. Sauvegarder et charger les données

### Sauvegarde manuelle

1. Allez dans **Fichier → 💾 Sauvegarder** (ou appuyez sur **Ctrl+S**).
2. Une fenêtre s'ouvre pour choisir un dossier de destination.
3. Entrez le **numéro de semaine** (ex : `19`).
4. L'application crée automatiquement un sous-dossier `S19/` avec vos données.

> 💡 La sauvegarde automatique fonctionne en permanence : chaque modification (affectation, modification d'un lot, etc.) est enregistrée immédiatement dans le dossier courant.

### Charger une sauvegarde précédente

1. Allez dans **Fichier → 📂 Ouvrir une sauvegarde…** (ou **Ctrl+O**).
2. Naviguez jusqu'au dossier de la semaine souhaitée (ex : `S19/`).
3. Sélectionnez ce dossier et cliquez sur **Ouvrir**.
4. Les données de cette semaine se chargent automatiquement.

---

## 9. Remettre à zéro (nouvelle semaine)

Pour commencer une nouvelle session vierge :

1. Allez dans **Fichier → 🆕 Nouveaux fichiers JSON…** (ou **Ctrl+N**).
2. Une confirmation vous est demandée : cliquez sur **Oui**.
3. Toutes les données sont effacées et l'application repart de zéro.

> ⚠️ **Attention** : pensez à sauvegarder la semaine en cours avant de faire cette action !

---

## 10. Questions fréquentes

**L'application ne démarre pas quand je double-clique sur `run.bat`.**
→ Vérifiez que Java est installé sur votre ordinateur. Faites un clic droit sur `run.bat` → *Ouvrir avec* → *Invite de commandes* pour voir le message d'erreur.

**Je ne vois pas mes lots après avoir remplacé le fichier Excel.**
→ Fermez et relancez l'application. Le fichier doit s'appeler exactement `export.xlsx` et être placé dans le dossier `app/data/`.

**Je ne peux pas supprimer un lot.**
→ Un lot affecté à une société ne peut pas être supprimé. Allez d'abord dans l'onglet **Affectation**, retirez le lot de sa société, puis revenez dans **Liste des lots** pour le supprimer.

**Les heures restantes d'une société sont incorrectes.**
→ Allez dans l'onglet **Sociétés & heures**, sélectionnez la société et cliquez sur **✏ Modifier** pour corriger le total d'heures initiales.

**J'ai fermé l'application sans sauvegarder, est-ce que j'ai perdu mes données ?**
→ Non. La sauvegarde automatique enregistre chaque modification au fur et à mesure dans le fichier courant (`app/data/courutilisation/`). Vos données sont conservées.

---

*Manuel rédigé pour les utilisateurs de l'application Planning Global Futura — PAM.*

## 11. Idées

## 1. Historique des actions (Undo / Redo) (très bonne idée)

- Implémenter le **pattern Command** pour annuler les dernières affectations ou modifications.
- Très utile en pratique.
- Fortement valorisé à l’oral pour démontrer la maîtrise des **design patterns**.

---

## 2. Tableau de bord avec indicateurs visuels (déjà fait en partie)

Créer un panneau récapitulatif avec :

- Nombre de lots affectés vs non affectés  
- Heures consommées par société  
- Barres de progression colorées :
  - 🟢 Vert : faible charge
  - 🟠 Orange : charge moyenne
  - 🔴 Rouge : proche de la saturation  

👉 Réalisable en **Swing** avec un `JProgressBar` stylisé.

---

## 3. Filtres avancés dans les tableaux (flèmme pas d'idée)

Améliorer le système de recherche actuel :

- Filtrer par statut  
- Filtrer par semaine  
- Filtrer par plage d’heures  
- Afficher uniquement les lots non affectés  

👉 Gain ergonomique important sans complexité technique élevée.

---

## 4. Export PDF/excel de la fiche de route (oui)

- Générer un export PDF de la **FicheRoute** existante.
- Utiliser une bibliothèque comme :
  - iText  
  - Apache PDFBox  

👉 Apporte un livrable concret et imprimable pour les sociétés.

---

## 5. Détection de conflit de capacité (bonne idée)

Avant d’affecter un lot :

- Ajouter un avertissement si une société dépasse un seuil (ex : 80% de capacité).
- Actuellement :
  - Blocage uniquement si 0 heure disponible ❌  
- Amélioration :
  - Avertissement progressif ⚠️  

👉 Meilleure ergonomie et anticipation des problèmes.

---

## 6. Vue "planning semaine" visuelle (m'oui)

Créer une vue de type calendrier :

- Lignes : sociétés  
- Colonnes : semaines  
- Contenu : charge de travail par société  

👉 Met en valeur les données temporelles déjà présentes dans les lots.
