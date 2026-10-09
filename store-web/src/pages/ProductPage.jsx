import React, { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import Stepper from '../Stepper';
import { api } from '../api';
import { documentHref, GROWING_DETAIL, GROWING_LABELS, imageSrc, TEST_RESULT_LABELS } from '../evidence';
import { money } from '../format';
import { useStore } from '../store';

const day = new Intl.DateTimeFormat('en-IN', { day: 'numeric', month: 'short', year: 'numeric' });

export default function ProductPage() {
  const { productId } = useParams();
  const { hub, catalog, loading } = useStore();
  const [detail, setDetail] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!hub) return;
    let stopped = false;
    setDetail(null);
    setError(null);
    api.product(hub.id, productId)
      .then((d) => { if (!stopped) setDetail(d); })
      .catch((e) => { if (!stopped) setError(e.message); });
    return () => { stopped = true; };
  }, [hub, productId]);

  if (error) {
    return <p className="notice error">{error} <Link to="/">Back to the shop</Link></p>;
  }
  if (loading || !detail) {
    return <p className="notice">Loading…</p>;
  }

  // The stepper works from the shop's own list, which also knows what is in the cart.
  const item = catalog.find((entry) => String(entry.productId) === String(productId));
  const photo = imageSrc(detail.imageUrl);
  const tested = detail.batches.filter((batch) => batch.testResult);

  return (
    <div className="narrow product">
      <p><Link to="/">← Back to the shop</Link></p>
      {photo && <img className="product-photo" src={photo} alt={detail.name} />}
      <h2>{detail.name}</h2>
      <p className="unit">{[detail.brand, detail.unitLabel, detail.category].filter(Boolean).join(' · ')}</p>
      {detail.description && <p>{detail.description}</p>}
      <div className="buy">
        <span className="price">{money(detail.pricePaise)}</span>
        {item && <Stepper item={item} />}
      </div>

      <h3>How it was grown</h3>
      {detail.growingMethod ? (
        <div className="evidence">
          <span className="badge">{GROWING_LABELS[detail.growingMethod]}</span>
          <p>{GROWING_DETAIL[detail.growingMethod]}</p>
          {detail.certifier && (
            <p className="unit">
              Certified by {detail.certifier}{detail.certificateNumber && `, certificate ${detail.certificateNumber}`}.
              {detail.certificateDocumentId && <> <a href={documentHref(detail.certificateDocumentId)} target="_blank" rel="noreferrer">Open the certificate</a></>}
            </p>
          )}
        </div>
      ) : (
        <p className="hint">We have no statement on file about how this product was grown.</p>
      )}

      <h3>Pesticide residue tests</h3>
      {detail.batches.length === 0 && <p className="hint">Nothing in stock right now.</p>}
      {detail.batches.length > 0 && tested.length === 0 && <p className="hint">The batches on sale today have not been lab tested.</p>}
      {tested.length > 0 && (
        <>
          <p className="hint">{tested.length} of the {detail.batches.length} {detail.batches.length === 1 ? 'batch' : 'batches'} on sale today {tested.length === 1 ? 'has' : 'have'} a lab test. Your order is picked from the batch that expires first.</p>
          <ul className="lines">
            {detail.batches.map((batch) => (
              <li key={batch.batchCode}>
                <div>
                  <strong>Batch {batch.batchCode}</strong>
                  <span className="unit">
                    Received {day.format(new Date(batch.receivedAt))}{batch.useBy && ` · use by ${day.format(new Date(batch.useBy))}`}
                  </span>
                  {batch.testResult ? (
                    <span className="unit">
                      {TEST_RESULT_LABELS[batch.testResult]} · {batch.testLab}, {day.format(new Date(batch.testDate))}
                      {batch.testDocumentId && <> · <a href={documentHref(batch.testDocumentId)} target="_blank" rel="noreferrer">Open the lab report</a></>}
                    </span>
                  ) : <span className="unit">Not tested</span>}
                </div>
              </li>
            ))}
          </ul>
        </>
      )}

      {detail.ingredients && (
        <>
          <h3>Ingredients</h3>
          <p>{detail.ingredients}</p>
        </>
      )}
      {detail.source === 'OFF' && (
        <p className="hint">
          Product details and photo from <a href={detail.sourceUrl} target="_blank" rel="noreferrer">Open Food Facts</a> contributors
          (data under the Open Database License, photo under CC BY-SA). Check the pack you receive.
        </p>
      )}
    </div>
  );
}
