# Ma Voix Écrite

Application Android gratuite, sans publicité et sans compte pour aider à communiquer lorsque parler est difficile ou impossible.

Projet bénévole en essai, présenté par l’**Équipe Ma Voix Écrite**. Son utilité pour d’autres personnes est à construire avec leurs retours ; aucun bénéfice clinique démontré n’est revendiqué.

**[Présentation et installation](https://mavoixecrite.github.io/ma-voix-ecrite/)** · **[Télécharger l’APK](https://mavoixecrite.github.io/ma-voix-ecrite/MaVoix.apk)**

## Pour qui ?

Pour les personnes qui ont besoin d’un autre moyen de s’exprimer, ponctuellement ou durablement. La présentation ne fixe pas de limite d’âge : les phrases, le niveau de lecture et l’accès tactile sont à adapter à chacun, avec un proche ou un professionnel si nécessaire. L’application n’a pas été évaluée pour tous les besoins ; elle ne convient pas automatiquement à toutes les situations.

## Ce que propose l’application

- Grandes cases regroupées en Essentiel, Besoins et Discussion : toucher une phrase pour la prononcer.
- Écriture libre, aide à la correction et suggestions de mots adaptées au texte et aux habitudes locales.
- Affichage du message en grand et carte de présentation retournable face à l’interlocuteur.
- Personnalisation des cases, favoris, historique local et sauvegarde dans un fichier choisi par l’utilisateur.
- Un même APK avec une interface téléphone et une interface tablette, adaptées à la largeur disponible.
- Sonnerie pour attirer l’attention à proximité. Elle n’est pas un appel distant ni une alarme médicale.

La synthèse vocale utilise une voix française hors ligne installée dans Android. Son téléchargement initial peut demander Internet ; les accents réellement disponibles dépendent du moteur vocal. La lecture et l’écriture fonctionnent ensuite hors ligne. Aucun enregistrement audio ni transcription de conversation n’est proposé.

## Installer et essayer

Android 8.0 ou plus récent. Téléchargez l’APK depuis les liens officiels ci-dessus, puis ouvrez-le. Si Android le demande, autorisez temporairement votre navigateur à installer cette application, puis retirez cette autorisation après l’installation. N’ignorez pas un avertissement de sécurité inattendu.

Ouvrez **Voix**, testez une phrase, puis recommencez en mode avion. Si la voix manque, installez une voix française hors ligne dans les réglages Android. L’affichage en grand reste une autre manière de transmettre le message.

Pour une mise à jour, installez par-dessus la version existante sans désinstaller : la continuité de signature est nécessaire pour conserver les données. Une sauvegarde locale est recommandée avant la mise à jour.

Version distribuée : **0.8.2**. L’empreinte de l’APK est publiée dans [SHA256.txt](SHA256.txt).

## Participer aux premiers essais

[Guide d’essai et questions de retour](ESSAI-ET-RETOURS.md) · [Fiche pour les personnes concernées et leurs proches](FICHE-PATIENT-ET-AIDANTS.md) · [Confidentialité](CONFIDENTIALITE.md)

Contact : **mavoixecrite@proton.me**. Les objets `[MAVOIX-RETOUR] [USAGE|BUG|IDEE|CONTRIB]` facilitent le tri. N’envoyez ni dossier médical, ni nom de personne, ni phrases privées. Les signalements GitHub sont publics.

## Sources et compilation

Les sources Android sont dans `app/`. Prérequis : JDK 17, Android SDK 35 et accès aux dépendances de compilation. Définissez `ANDROID_HOME` vers votre SDK ou créez un `local.properties` privé avec `sdk.dir`.

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug --console=plain
```

Sous Linux/macOS : `./gradlew assembleDebug testDebugUnitTest lintDebug`.
L’APK produit localement est `app/build/outputs/apk/debug/app-debug.apk`. Une compilation personnelle utilise votre propre clé : elle ne remplace pas nécessairement l’APK officiel. Aucune clé privée de signature n’est publiée.

Package : `fr.mavoixecrite`. Interface compacte sous 600 dp et tablette à partir de 600 dp. Tests Robolectric sur API 26 et 35. Les tests vérifient le comportement logiciel, sans constituer une validation clinique.

Voir [CONTRIBUTING.md](CONTRIBUTING.md). Aucune présence sur Google Play ou F-Droid n’est annoncée à ce stade.

## Licences et limites

Code sous [licence MIT](LICENSE). L’extrait lexical Lexique 3.83 conserve la licence **CC BY-SA 4.0** et ses attributions dans `app/src/main/assets/LEXIQUE-ATTRIBUTION.txt`. La licence MIT du code ne remplace pas celle des données tierces.

Outil d’aide à la communication, dont l’adaptation doit être appréciée avec la personne concernée et, si nécessaire, un professionnel. Il ne remplace ni un suivi médical ou orthophonique ni un système d’appel d’urgence. Conservez un autre moyen de communiquer.
