# Group Formation for Autistic Users in Tourism Recommender Systems

## Repository Structure

```
release/
├── webapp/             Web application (backend + frontend)
│   ├── java/           Java servlet backend (Jakarta EE)
│   ├── frontend/       Vue 3 frontend (Vite)
│   ├── webapp_static/  HTML entry point
│   ├── pom.xml         Maven build (production)
│   ├── pom_dev.xml     Maven build (development)
│   └── pom_prod.xml    Maven build (alternate production)
│
└── data/
    ├── activities.csv              128 leisure/tourism activities with sensory profiles
    ├── candidate_profiles.csv      200 synthetic candidate persons (Big Five + SPQ)
    └── study_data/
        ├── participant_profiles.csv           One row per participant: raw subscale scores (200 rows)
        ├── participant_bigfive_responses.csv  Item-level BFI-10 answers (2 000 rows)
        ├── participant_spq_responses.csv      Item-level Short SPQ answers (7 000 rows)
        ├── candidate_ratings.csv              Per-round candidate ratings (3 600 rows)
        └── group_session_features.csv         Session-level group composition features (400 rows)
```

---

## Web Application (`webapp/`)

The study was conducted via a custom web app built with:

- **Backend**: Java servlets (Jakarta EE / Tomcat). Entry points in `java/com/example/prolificstudy2/`. Data model classes in `java/datamodel/`.
- **Frontend**: Vue 3 + Vite. Components in `frontend/src/components/`, views in `frontend/src/views/`. Translations (EN/IT) in `frontend/src/translations/`.

### Building

```bash
# Build frontend assets
cd webapp/frontend
npm install
npm run build          # outputs to ../webapp_static/static/

# Build and deploy backend
cd ..
mvn -f pom.xml package
# Deploy the generated WAR to Tomcat
```

---

## Data Files

### `data/activities.csv` — Activity Dataset (128 rows)

Each row is one leisure/tourism activity. Columns:

| Column | Description                                                                                                                                         |
|--------|-----------------------------------------------------------------------------------------------------------------------------------------------------|
| `id` | Activity identifier                                                                                                                                 |
| `title_english` | Activity title                                                                                                                                      |
| `description_english` | Short description                                                                                                                                   |
| `category` | One of 7 categories: *Sport and well being, Live music, Courses and workshops, Parks and nature, Museums and culture, Cinema and shows, Gastronomy* |
| `city` | City (removed for double blind)                                                                                                                     |
| `nr_like` | Number of "likes" received during the study                                                                                                         |
| `spq_vision_score` … `spq_taste_score` | Sensory demand scores 0–100 for each SPQ modality                                                                                                   |
| `spq_vision_description` … `spq_taste_description` | Natural-language rationale for each score                                                                                                           |

### `data/candidate_profiles.csv` — Candidate Profiles (200 rows)

200 synthetic candidate persons generated to span the trait space. Columns:

| Column | Description |
|--------|-------------|
| `candidate_id` | Database identifier (matches `candidate_id` in `candidate_ratings.csv`) |
| `unique_id` | UUID |
| `spq_total` | SPQ total score |
| `spq_vision` … `spq_touch` | SPQ subscores per modality |
| `bigf_total` | BFI-10 total score |
| `bigf_agreeableness` … `bigf_openness` | Big Five subscores |

### `data/study_data/participant_profiles.csv` — Participant Profiles (200 rows)

One row per study participant (only those who completed the study). Columns:

| Column | Description |
|--------|-------------|
| `user_id` | Participant identifier (matches `user_id` in all study_data files) |
| `neurotype` | Self-reported neurotype: *autistic / neurotypical* |
| `spq_total` | SPQ total raw score |
| `spq_vision` … `spq_touch` | SPQ subscale raw scores per modality |
| `bigf_total` | BFI-10 total raw score |
| `bigf_extroversion` … `bigf_openness` | Big Five subscale raw scores |

Note: the `u_bigf_*` and `u_spq_*` columns in `candidate_ratings.csv` are z-scored versions of these subscale scores (standardised across all participants). This file provides the raw scores.

### `data/study_data/participant_bigfive_responses.csv` — BFI-10 Item Responses (2 000 rows)

Individual answers to each of the 10 BFI-10 items, for all 200 participants (10 × 200 = 2 000 rows). Columns:

| Column | Description |
|--------|-------------|
| `user_id` | Participant identifier |
| `question_id` | Item number (1–10) |
| `question_text` | English item text (e.g. "I see myself as someone who is reserved") |
| `category` | Big Five trait: *Extroversion, Agreeableness, Conscientiousness, Emotional stability, Openness* |
| `inverted` | 1 if this item is reverse-scored, 0 otherwise |
| `answer` | Raw response (0–4 Likert; higher = more agreement) |

### `data/study_data/participant_spq_responses.csv` — Short SPQ Item Responses (7 000 rows)

Individual answers to each of the 35 Short SPQ items, for all 200 participants (35 × 200 = 7 000 rows). Columns:

| Column | Description |
|--------|-------------|
| `user_id` | Participant identifier |
| `question_id` | Item number (1–35) |
| `question_text` | English item text |
| `main_category` | SPQ modality: *Vision, Hearing, Smell, Taste, Touch* |
| `sub_category` | Sub-dimension within the modality (e.g. *Brightness, Food, Salty*) |
| `inverted` | 1 if this item is reverse-scored, 0 otherwise |
| `answer` | Raw response (0–4 Likert; higher = more sensitivity) |

### `data/study_data/candidate_ratings.csv` — Per-Round Ratings (3 600 rows)

One row per (participant × activity × round × candidate) evaluation. Columns:

| Column | Description |
|--------|-------------|
| `user_id` | Participant identifier |
| `activity_id` | Activity identifier |
| `round_number` | Round within the activity session (1–3) |
| `candidate_id` | Candidate being rated |
| `candidate_type` | Strategy that generated this candidate: *similar / complementary / random* |
| `is_assigned` | 1 if participant chose this candidate, 0 otherwise |
| `assigned_candidate_id` | ID of the candidate actually chosen this round |
| `act_category` | Activity category |
| `neurotype` | Participant self-report: *autistic / neurotypical* |
| `q_compatible_with_user` | CR1: "Compatible with me" (1–5) |
| `q_sensory_suitable` | CR2: "Sensory-suitable for me" (1–5) |
| `q_compatible_with_group` | CR3: "Compatible with the group" (1–5) |
| `q_adds_to_group` | CR4: "Adds value to the group" (1–5) |
| `q_comfort` | CR5: "I'd feel comfortable" (1–5) |
| `q_personality_compat` | CR6: "Personality compatible" (1–5) |
| `q_sensory_compat` | CR7: "Sensory compatible" (1–5) |
| `mean_rating` | Mean of CR1–CR7 |
| `group_size` | Number of members in the group at this round |
| `u_bigf_*` | Participant Big Five z-scores (5 traits) |
| `u_spq_*` | Participant SPQ z-scores (5 modalities) |
| `c_bigf_*` | Candidate Big Five z-scores |
| `c_spq_*` | Candidate SPQ z-scores |
| `act_like` | How much participant liked the activity (1–5) |
| `act_group_want` | How much participant wanted to do it with a group (1–5) |
| `act_compat_imp` | Importance of personality compatibility (1–5) |
| `act_div_imp` | Importance of group diversity (1–5) |
| `act_spq_*` | Activity SPQ weight per modality (normalised 0–1) |
| `similarity_score` | Cosine similarity between participant and candidate (combined Big Five + SPQ) |
| `complementarity_score` | Weighted squared distance (heterogeneity contribution) |
| `pw_sim_min/mean/max/sum/…` | Group-level pairwise similarity aggregations (combined profile) |
| `pw_comp_min/mean/max/sum/…` | Group-level pairwise complementarity aggregations |
| `pw_sim_*_b5` | Same aggregations computed from Big Five only |
| `pw_sim_*_spq` | Same aggregations computed from SPQ only |
| `pw_comp_*_b5` | Complementarity aggregations from Big Five only |
| `pw_comp_*_spq` | Complementarity aggregations from SPQ only |

### `data/study_data/group_session_features.csv` — Session-Level Features (400 rows)

Aggregate features of the final formed group per session:

| Column | Description |
|--------|-------------|
| `user_id`, `activity_id` | Session identifiers |
| `group_size` | Final group size |
| `group_bigf_diversity` | Mean pairwise Euclidean distance in Big Five space |
| `group_spq_diversity` | Mean pairwise Euclidean distance in SPQ space |
| `user_dist_bigf` | Participant's distance to group centroid (Big Five) |
| `user_dist_spq` | Participant's distance to group centroid (SPQ) |
| `group_mean_bigf_*` | Group mean per Big Five trait |
| `group_var_bigf_*` | Group variance per Big Five trait |
| `group_mean_spq_*` | Group mean per SPQ modality |
| `group_var_spq_*` | Group variance per SPQ modality |
| `n_assigned_similar` | Number of similar candidates chosen |
| `n_assigned_complementary` | Number of complementary candidates chosen |
| `n_assigned_random` | Number of random candidates chosen |
| `fg_*` | Final group evaluation items (GE1–GE7 + mean) |
| `neurotype` | Participant self-report |