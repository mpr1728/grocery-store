import React from 'react';
export default function MembershipPlans() {
  const plans = [
    { name: 'Basic', price: 0 },
    { name: 'Silver', price: 4.99 },
    { name: 'Gold', price: 9.99 }
  ];
  return (
    <div>
      <h2>Membership Plans</h2>
      <ul>
        {plans.map(plan => (
          <li key={plan.name}>{plan.name} - ${plan.price}/mo</li>
        ))}
      </ul>
    </div>
  );
}
