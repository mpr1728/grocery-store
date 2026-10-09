import React, { useCallback, useEffect, useState } from 'react';
import { api } from '../api';
import { documentHref, GROWING_LABELS, imageSrc, TEST_RESULT_LABELS } from '../evidence';

const today = () => new Date().toISOString().slice(0, 10);

function explain(e) {
  return e.details && e.details.length ? `${e.message}: ${e.details.join('; ')}` : e.message;
}

/** One stock row's residue test: what is on file, and a small form to add or replace it. */
export function BatchTest({ hubId, batch, onSaved }) {
  const [editing, setEditing] = useState(false);
  const [lab, setLab] = useState(batch.testLab || '');
  const [testDate, setTestDate] = useState(batch.testDate || today());
  const [result, setResult] = useState(batch.testResult || 'NONE_DETECTED');
  const [report, setReport] = useState(null);
  const [problem, setProblem] = useState(null);
  const [saving, setSaving] = useState(false);

  async function save(event) {
    event.preventDefault();
    setSaving(true);
    setProblem(null);
    try {
      await api.recordBatchTest(hubId, batch.batchId, { lab, testDate, result, report });
      setEditing(false);
      onSaved();
    } catch (e) {
      setProblem(explain(e));
    } finally {
      setSaving(false);
    }
  }

  if (!editing) {
    return (
      <>
        {batch.testResult ? (
          <span className="unit">
            {TEST_RESULT_LABELS[batch.testResult]}<br />{batch.testLab}, {batch.testDate}
            {batch.testDocumentId && <> · <a href={documentHref(batch.testDocumentId)} target="_blank" rel="noreferrer">report</a></>}
          </span>
        ) : <span className="unit">Not tested</span>}
        {' '}<button className="link" onClick={() => setEditing(true)}>{batch.testResult ? 'Change' : 'Add test'}</button>
      </>
    );
  }
  return (
    <form className="inline-form" onSubmit={save}>
      <input aria-label="Lab" placeholder="Lab name" maxLength={100} value={lab} onChange={(e) => setLab(e.target.value)} required />
      <input aria-label="Test date" type="date" max={today()} value={testDate} onChange={(e) => setTestDate(e.target.value)} required />
      <select aria-label="Result" value={result} onChange={(e) => setResult(e.target.value)}>
        {Object.entries(TEST_RESULT_LABELS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
      </select>
      <input aria-label="Lab report" type="file" accept="application/pdf,image/jpeg,image/png" onChange={(e) => setReport(e.target.files[0] || null)} />
      <button className="small" disabled={saving}>Save</button>
      <button type="button" className="link" onClick={() => setEditing(false)}>Cancel</button>
      {problem && <span className="field-error">{problem}</span>}
    </form>
  );
}

function GrowingRow({ hubId, item, onSaved }) {
  const [form, setForm] = useState(null);
  const [problem, setProblem] = useState(null);
  const [saving, setSaving] = useState(false);

  async function edit() {
    setProblem(null);
    try {
      const detail = await api.product(hubId, item.productId);
      setForm({
        growingMethod: detail.growingMethod || '',
        certifier: detail.certifier || '',
        certificateNumber: detail.certificateNumber || '',
        certificate: null,
        onFile: detail.certificateDocumentId
      });
    } catch (e) {
      setProblem(explain(e));
    }
  }

  async function save(event) {
    event.preventDefault();
    setSaving(true);
    setProblem(null);
    try {
      // Empty fields are left out of the upload, and a missing method clears the claim.
      await api.setGrowing(item.productId, {
        growingMethod: form.growingMethod,
        certifier: form.certifier,
        certificateNumber: form.certificateNumber,
        certificate: form.certificate
      });
      setForm(null);
      onSaved();
    } catch (e) {
      setProblem(explain(e));
    } finally {
      setSaving(false);
    }
  }

  return (
    <tr>
      <td>{item.name} <span className="unit">{[item.brand, item.unitLabel].filter(Boolean).join(' · ')}</span></td>
      <td>
        {!form && (
          <>
            {item.growingMethod ? <span className="badge">{GROWING_LABELS[item.growingMethod]}</span> : <span className="unit">Not stated</span>}
            {' '}<button className="link" onClick={edit}>Edit</button>
            {problem && <span className="field-error"> {problem}</span>}
          </>
        )}
        {form && (
          <form className="inline-form" onSubmit={save}>
            <select aria-label="How it was grown" value={form.growingMethod} onChange={(e) => setForm({ ...form, growingMethod: e.target.value })}>
              <option value="">Not stated</option>
              <option value="CERTIFIED_ORGANIC">Certified organic (needs a certificate)</option>
              <option value="GROWER_DECLARED">No synthetic pesticides (grower's declaration)</option>
            </select>
            <input aria-label="Certifier" placeholder="Certifier" maxLength={100} value={form.certifier} onChange={(e) => setForm({ ...form, certifier: e.target.value })} />
            <input aria-label="Certificate number" placeholder="Certificate number" maxLength={60} value={form.certificateNumber} onChange={(e) => setForm({ ...form, certificateNumber: e.target.value })} />
            <input aria-label="Certificate file" type="file" accept="application/pdf,image/jpeg,image/png" onChange={(e) => setForm({ ...form, certificate: e.target.files[0] || null })} />
            {form.onFile && <a href={documentHref(form.onFile)} target="_blank" rel="noreferrer">certificate on file</a>}
            <button className="small" disabled={saving}>Save</button>
            <button type="button" className="link" onClick={() => setForm(null)}>Cancel</button>
            {problem && <span className="field-error">{problem}</span>}
          </form>
        )}
      </td>
    </tr>
  );
}

/** Per product: how it was grown and the certificate behind it. */
export function GrowingTab({ hubId, catalog, onSaved }) {
  return (
    <>
      <p className="hint">
        Shoppers see exactly what you record here, with the certificate. Only choose "certified organic" when you hold the
        certificate. Lab tests are recorded per batch on the Stock tab. Files: PDF, JPEG, or PNG up to 4 MB.
      </p>
      <div className="table-wrap">
        <table>
          <thead><tr><th>Product</th><th>How it was grown</th></tr></thead>
          <tbody>
            {catalog.map((item) => <GrowingRow key={item.productId} hubId={hubId} item={item} onSaved={onSaved} />)}
          </tbody>
        </table>
      </div>
    </>
  );
}

function Candidate({ hubId, candidate, onDone }) {
  const [price, setPrice] = useState('');
  const [diet, setDiet] = useState(candidate.diet || '');
  const [category, setCategory] = useState(candidate.category || '');
  const [unitLabel, setUnitLabel] = useState(candidate.unitLabel || '');
  const [problem, setProblem] = useState(null);
  const [busy, setBusy] = useState(false);

  async function decide(action) {
    setBusy(true);
    setProblem(null);
    try {
      await action();
      onDone();
    } catch (e) {
      setProblem(explain(e));
      setBusy(false);
    }
  }

  const approve = (event) => {
    event.preventDefault();
    decide(() => api.approveCandidate(hubId, candidate.id, {
      pricePaise: Math.round(Number(price) * 100), diet, category, unitLabel
    }));
  };

  return (
    <article className="candidate">
      {candidate.imageUrl ? <img src={imageSrc(candidate.imageUrl)} alt="" loading="lazy" /> : <span />}
      <div>
        <h3>{candidate.name}</h3>
        <p className="unit">
          {[candidate.brand, candidate.unitLabel, candidate.labels].filter(Boolean).join(' · ')}
          {candidate.sourceUrl && <> · <a href={candidate.sourceUrl} target="_blank" rel="noreferrer">source</a></>}
        </p>
        {candidate.ingredients && <p className="desc">Ingredients: {candidate.ingredients}</p>}
        <form className="inline-form" onSubmit={approve}>
          <input aria-label="Price in rupees" type="number" min="1" step="0.5" placeholder="Price ₹" value={price} onChange={(e) => setPrice(e.target.value)} required />
          <select aria-label="Diet" value={diet} onChange={(e) => setDiet(e.target.value)} required>
            <option value="">Veg or non-veg?</option>
            <option value="VEG">Vegetarian</option>
            <option value="NON_VEG">Non-vegetarian</option>
          </select>
          <input aria-label="Category" placeholder="Category" maxLength={50} value={category} onChange={(e) => setCategory(e.target.value)} required />
          <input aria-label="Pack size" placeholder="Pack size" maxLength={30} value={unitLabel} onChange={(e) => setUnitLabel(e.target.value)} required />
          <button className="small" disabled={busy}>Add to shop</button>
          <button type="button" className="small quiet" disabled={busy} onClick={() => decide(() => api.rejectCandidate(candidate.id))}>Reject</button>
          {problem && <span className="field-error">{problem}</span>}
        </form>
      </div>
    </article>
  );
}

/** Products found by an import. Nothing reaches the shop until someone checks it and sets a price here. */
export function ImportTab({ hubId, onChanged }) {
  const [page, setPage] = useState(null);
  const [error, setError] = useState(null);

  const load = useCallback(async () => {
    try {
      setPage(await api.candidates());
      setError(null);
    } catch (e) {
      setError(e.message);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  if (error) return <p className="notice error">{error}</p>;
  if (!page) return <p className="notice">Loading…</p>;
  if (page.waiting === 0) return <p className="notice">Nothing is waiting for review.</p>;

  return (
    <>
      <p className="hint">
        {page.waiting} waiting{page.waiting > page.candidates.length && `, showing the first ${page.candidates.length}`}.
        These come from Open Food Facts, where anyone can add or edit entries, so check the name, ingredients, and
        whether it is vegetarian against the real pack. A product you add starts with no stock.
      </p>
      <div className="candidates">
        {page.candidates.map((candidate) => (
          <Candidate key={candidate.id} hubId={hubId} candidate={candidate} onDone={() => { load(); onChanged(); }} />
        ))}
      </div>
    </>
  );
}
