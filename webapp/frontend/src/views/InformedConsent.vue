<template>
  <div class="informed-consent">
    <h2>{{ t.title }}</h2>

    <div class="consent-content">
      <p class="placeholder-text" v-html="t.placeholderText"></p>
    </div>

    <div class="consent-checkboxes">
      <label class="checkbox-item">
        <input type="checkbox" v-model="isAdult" />
        <span>{{ t.adultDeclaration }}</span>
      </label>

      <label class="checkbox-item">
        <input type="checkbox" v-model="agreedToStudy" />
        <span>{{ t.studyAgreement }}</span>
      </label>
    </div>

    <button
      class="btn-consent"
      @click="submit"
      :disabled="!isValid"
      :title="!isValid ? t.validationError : ''"
    >
      {{ t.continue }}
    </button>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useLanguageStore } from '@/stores/language.js'

const store = useLanguageStore()
const emit = defineEmits(['submit'])

const isAdult = ref(false)
const agreedToStudy = ref(false)
const isValid = computed(() => isAdult.value && agreedToStudy.value)

const translations = {
  en: {
    title: 'Informed Consent',
    placeholderText: '<strong>Consent form</strong><br>This study is in line with the current laws D. Lgs 196/2003 and EU GDPR 679/2016 on data protection and by performing the study you consent to the processing and communication of personal data, within the limits, for the purposes and for the duration specified by the current laws (D. Lgs 196/2003 and EU GDPR 679/2016). The data collection complies with the obligations laid down by current legislation in terms of the collection, processing, and storage of sensitive data.<br><br><strong>Potential risks:</strong><br>There are no known or anticipated risks to you by participating in this research.<br><br><strong>Confidentiality:</strong><br>No personally identifiable information will be collected, and although the data from this research project will be published and presented at conferences or journals, it will not be possible to identify individuals.<br><br><strong>Recruitment:</strong><br>You have been recruited through Prolific.com or through word-of-mouth.<br><br><strong>Storage of Data:</strong><br>During the study, the collected data will be electronically saved on Data Processor/s Aruba s.p.a, via San Clemente 52, 24036 Ponte San Pietro (BG), ITALY. Moreover, this data will be subsequently stored on the storage service offered by the University of XYZ. The data will be stored for a period that is no longer than what is needed for the present research, then it will be destroyed.<br><br><strong>Right to Withdraw:</strong><br>The participation in this study is voluntary. You can decide not to participate at any time by closing your browser and contacting the principal investigator — please include the assigned ID to withdraw. Your right to withdraw data from the study will apply before the data analysis phase, which will happen after the end of the study on July 1st, 2026.<br><br><strong>Questions or Concerns:</strong><br>Contact the principal investigator, Noemi Mauro (noemi.mauro@unito.it).<br><br>By completing and submitting this form, your free and informed consent is implied.',
    adultDeclaration: 'I declare to be at least 18 years old.',
    studyAgreement: 'I agree to participate in the study and to the processing of the data collected during the test.',
    continue: 'Continue →',
    validationError: 'Please check both boxes to continue'
  },
  it: {
    title: 'Consenso Informato',
    placeholderText: '<strong>Modulo di consenso</strong><br>Questo studio è in linea con le vigenti leggi D. Lgs 196/2003 e GDPR UE 679/2016 sulla protezione dei dati. Partecipando allo studio, acconsenti al trattamento e alla comunicazione dei dati personali, nei limiti, per le finalità e per la durata specificati dalla normativa vigente.<br><br><strong>Rischi potenziali:</strong><br>Non ci sono rischi noti o previsti per la tua partecipazione a questa ricerca.<br><br><strong>Riservatezza:</strong><br>Non verranno raccolte informazioni personalmente identificabili. I dati di questa ricerca saranno pubblicati e presentati a conferenze o su riviste, ma non sarà possibile identificare i singoli partecipanti.<br><br><strong>Reclutamento:</strong><br>Sei stato reclutato tramite Prolific.com o tramite passaparola.<br><br><strong>Conservazione dei dati:</strong><br>Durante lo studio, i dati raccolti saranno salvati elettronicamente presso Aruba s.p.a, via San Clemente 52, 24036 Ponte San Pietro (BG), ITALIA. Successivamente, saranno conservati nel servizio di archiviazione offerto dall\'Università di XYZ. I dati saranno conservati per il periodo strettamente necessario alla ricerca, dopodiché saranno distrutti.<br><br><strong>Diritto di recesso:</strong><br>La partecipazione a questo studio è volontaria. Puoi decidere di non partecipare in qualsiasi momento chiudendo il browser e contattando il responsabile principale — includi l\'ID assegnato per effettuare il recesso. Il diritto di ritiro dei dati si applica prima della fase di analisi dei dati, che avverrà dopo la fine dello studio il 1 luglio 2026.<br><br><strong>Domande o dubbi:</strong><br>Contatta i responsabili principali: Noemi Mauro (noemi.mauro@unito.it).<br><br>Completando e inviando questo modulo, il tuo consenso libero e informato è implicito.',
    adultDeclaration: 'Dichiaro di avere almeno 18 anni.',
    studyAgreement: 'Acconsento a partecipare allo studio e al trattamento dei dati raccolti durante il test.',
    continue: 'Continua →',
    validationError: 'Seleziona entrambe le caselle per continuare'
  }
}

const t = computed(() => translations[store.currentLanguage] || translations.en)

function submit() {
  if (isValid.value) {
    sessionStorage.setItem('consentGiven', 'true')
    emit('submit')
  }
}
</script>

<style scoped>
.informed-consent {
  padding: 1.5rem 1rem;
  max-width: 800px;
  margin: 0 auto;
}

h2 {
  color: #007ACC;
  margin-bottom: 1.5rem;
  font-size: 1.5rem;
}

.consent-content {
  background-color: #f8f9fa;
  border: 1px solid #dee2e6;
  border-radius: 8px;
  padding: 1.5rem;
  margin-bottom: 1.5rem;
  max-height: 350px;
  overflow-y: auto;
}

.placeholder-text {
  color: #444;
  line-height: 1.7;
  margin: 0;
  font-size: 0.92rem;
}

.consent-checkboxes {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  margin-bottom: 1.5rem;
  padding: 1rem;
  background-color: #fff;
  border: 1px solid #e9ecef;
  border-radius: 8px;
}

.checkbox-item {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  cursor: pointer;
  padding: 0.4rem 0.5rem;
  border-radius: 4px;
  transition: background-color 0.2s ease;
}

.checkbox-item:hover { background-color: #f8f9fa; }

.checkbox-item input[type="checkbox"] {
  margin-top: 0.2rem;
  width: 18px;
  height: 18px;
  cursor: pointer;
  flex-shrink: 0;
  accent-color: #007ACC;
}

.checkbox-item span {
  color: #333;
  line-height: 1.5;
  font-size: 1rem;
}

.btn-consent {
  width: 100%;
  padding: 0.875rem 2rem;
  background: #007ACC;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 1.1rem;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s ease, transform 0.1s ease;
}

.btn-consent:hover:not(:disabled) {
  background: #005fa3;
  transform: translateY(-1px);
}

.btn-consent:disabled {
  opacity: 0.45;
  cursor: not-allowed;
  transform: none;
}
</style>
