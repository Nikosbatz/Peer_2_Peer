# Προγραμματιστική Άσκηση Υλοποίησης Κατανεμημένου Συστήματος Ομότιμων (P2P) για την ανταλλαγή αρχείων.</br>
**Σύντομη ανάλυση της υλοποίησης.**

Η υλοποίηση αναπτύχθηκε για ένα απλό σύστημα peer-to-peer (P2P) δικτύου. Οι συμμετέχοντες -peers- μπορούν να εγγραφούν, να συνδεθούν, να δουν τα διαθέσιμα αρχεία, να ζητήσουν λεπτομέρειες για ένα αρχείο και να το κατεβάσουν από άλλους συμμετέχοντες. Η επικοινωνία γίνεται μέσω ενός κεντρικού tracker και μεταξύ των peer. Υπάρχουν μηχανισμοί αντιμετώπισης σφαλμάτων και επιλογής του επόμενου καλύτερου peer σε περίπτωση αποτυχίας λήψης αρχείου από έναν peer. 
## Σύνοψη των Αρχείων

Περιγραφή των αρχείων που αποτελούν την εφαρμογή, οδηγίες μεταγλωττίζονται που προκύπτουν, καθώς και πληροφορίες σχετικά με το πως σχετίζονται, πώς και ποια είναι τα αρχεία εξόδου.

- **Peer.java**: Αντιπροσωπεύει έναν peer στο δίκτυο. Περιλαμβάνει λειτουργίες για εγγραφή, σύνδεση, κατάλογο αρχείων, λήψη λεπτομερειών αρχείου, λήψη αρχείου και διαχείριση επικοινωνίας με άλλους peers και τον tracker.
- **Peer/Clienthandler.java**: Υλοποιεί τις παραπάνω λειτουργίες επικοινωνίας του peer με τον tracker ή με άλλον peer.
- **Message.java**: Ορίζει τη μορφή μηνυμάτων για την επικοινωνία μεταξύ των peers και του tracker.
- **PeerInfo.java**: Περιέχει πληροφορίες για έναν peer, όπως το όνομα χρήστη, η διεύθυνση IP και ο αριθμός θύρας.
- **PeerServer.java**: Αντιπροσωπεύει έναν server που τρέχει από κάθε peer για την αντιμετώπιση εισερχόμενων αιτημάτων από άλλους peers ή τον tracker.
- **Tracker.java**: Αντιπροσωπεύει τον κεντρικό tracker στο P2P δίκτυο. Διαχειρίζεται την εγγραφή των peers, την είσοδο, και τον κατάλογο των αρχείων.
- **Tracker/Clienthandler.java**: Υλοποιεί τις μεθόδους επικοινωνίας του tracker καθώς και τις ζητούμενες δομές για να διαχειρίζεται τα αιτήματα από τους peers.

## Τεκμηρίωση της Εφαρμογής

Αναφορά στην εκτέλεση της εφαρμογής, προβλήματα που αντιμετωπίσατε, αποκλίσεις από τις προδιαγραφές κ.λπ.

### Τρόπος Εκτέλεσης της Εφαρμογής:

1. Εκτελέστε τον Tracker Server.
2. Συνδέστε τους Peers στον Tracker Server, είτε τοπικά είτε μέσω δικτύου, αλλάζοντας τα πεδία ports και το shared directory για κάθε έναν peer στην κλάση Peer πριν τον τρέξετε.
3. Οι Peers μπορούν να εγγραφούν, να συνδεθούν, να αποσυνδεθούν, να λάβουν κατάλογο αρχείων, να ζητήσουν λεπτομέρειες αρχείων και να ειδοποιήσουν τον Tracker για επιτυχείς ή αποτυχημένες λήψεις.
4. Ο Tracker Server διαχειρίζεται αυτά τα αιτήματα, διατηρώντας τις καταχωρήσεις των Peers και παρέχοντας τις κατάλληλες απαντήσεις.
