# Πρόγραμμα οδήγησης βίντεο FFmpeg

Το **Πρόγραμμα οδήγησης βίντεο FFmpeg** συνδέεται σε ροή κάμερας IP (RTSP/HTTP) ή σε αρχείο βίντεο, παρέχει ζωντανό βίντεο χαμηλής καθυστέρησης στο OSCAR Viewer και εξάγει διατηρούμενα καρέ τεκμηρίωσης JPEG (`frame-01.jpg` έως `frame-05.jpg`) όταν προκύπτει συναγερμός διέλευσης.

## Επεξήγηση όρων σε απλή γλώσσα

| Όρος | Σημασία |
| --- | --- |
| **Καρέ-κλειδί / Διάστημα GOP (`1–2 s`)** | Ένα πλήρες καρέ αναφοράς βίντεο (`I-frame`). Συνιστάται η ρύθμιση της κάμερας με καρέ-κλειδί κάθε `1–2 δευτερόλεπτα` (`GOP 30–60` στα `30 fps`) για άμεση εκκίνηση της ζωντανής ροής και ακριβή λήψη στιγμιοτύπων συναγερμού. |
| **Διατηρούμενα πολυμέσα 5 καρέ** | Για κάθε διέλευση με συναγερμό, το OSCAR εξάγει 5 ισοκατανεμημένα καρέ JPEG. Μετά από 30 ημέρες (`retainedSecondaryFramesDays = 30`), τα δευτερεύοντα καρέ (`01`, `02`, `04`, `05`) διαγράφονται ενώ το κεντρικό καρέ (`frame-03.jpg`) διατηρείται. |
| **Τοπικός διαμεσολαβητής RTSP (`mediamtx`)** | Όταν η λειτουργία ζωντανής ροής στην Υπηρεσία OSCAR είναι `RTSP_PROXY` ή `HYBRID`, το OSCAR προωθεί τα καρέ H.264 στον τοπικό διακομιστή `mediamtx` (`127.0.0.1:8554`) ώστε πολλοί χειριστές να παρακολουθούν ζωντανά χωρίς πολλαπλές συνδέσεις προς τη φυσική κάμερα. |

## Σύνδεση

- **Συμβολοσειρά σύνδεσης (`Connection String`)** / **Διαδρομή αρχείου (`File Path`)**: Ορίστε τη συμβολοσειρά σύνδεσης για ζωντανή ροή RTSP/HTTP ή τη διαδρομή αρχείου για εγγεγραμμένο βίντεο.
- **Χρήση TCP (`Use TCP`)**: Ενεργοποιήστε το TCP για κάμερες RTSP όταν απαιτείται αξιόπιστη μεταφορά.
- **Παράβλεψη χρονικών σημάνσεων δεδομένων (`Ignore Data Timestamps`)**: Ενεργοποιήστε το όταν το ρολόι της κάμερας δεν είναι αξιόπιστο ώστε να χρησιμοποιείται η ώρα του διακομιστή.

## Έξοδοι

- **Καρέ βίντεο (`Video Frames`)**: Δημοσιεύει καρέ βίντεο H.264 για ζωντανή προβολή και εξαγωγή των 5 καρέ τεκμηρίωσης συναγερμών.
- **HLS**: Δημοσιεύει έξοδο HLS όταν απαιτείται.
- Ρυθμίστε το αναγνωριστικό ροής, το χρονικό όριο σύνδεσης και τη συμπεριφορά επανασύνδεσης όπως απαιτείται.

<details class="oscar-video-guide">
  <summary>Βιντεοοδηγός (#11, 12.0s): Επιθεώρηση ζωντανού βίντεο λωρίδας &amp; καμπυλών γάμμα/νετρονίων</summary>
  <video controls loading="lazy" width="720" height="405" preload="none" playsinline>
    <source src="/video-guides/videos/11-live-lane-video-and-charts.mp4" type="video/mp4">
    <track kind="captions" src="/video-guides/captions/11-live-lane-video-and-charts.vtt" srclang="en" label="English" default>
  </video>
</details>

<details class="oscar-video-guide">
  <summary>Βιντεοοδηγός (#13, 19.4s): Έλεγχος διατηρούμενων 5 καρέ, ημερήσιου αρχείου &amp; υποβολή αξιολόγησης</summary>
  <video controls loading="lazy" width="720" height="405" preload="none" playsinline>
    <source src="/video-guides/videos/13-event-details-adjudication-and-daily-file.mp4" type="video/mp4">
    <track kind="captions" src="/video-guides/captions/13-event-details-adjudication-and-daily-file.vtt" srclang="en" label="English" default>
  </video>
</details>
