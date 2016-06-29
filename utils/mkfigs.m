figure; 

subplot(221); hold; 
scatter(toI(~ei), toE(~ei), 4*FiringRates(~ei)+2);
scatter(toI(ei), toE(ei),4*FiringRates(ei)+2);
plot([0 500], [0 500], 'k');
plot([209 209], [0 500], 'k--');
hold;

subplot(222); hold; 
scatter(toIjc(~eijc), toEjc(~eijc), 4*FiringRatesjc(~eijc)+2);
scatter(toIjc(eijc), toEjc(eijc),4*FiringRatesjc(eijc)+2);
plot([0 400], [0 400], 'k');
plot([219 219], [0 400], 'k--');
hold;

subplot(223); hold;
scatter(kIn(~ei), kOut(~ei), 4*FiringRates(~ei)+2);
scatter(kIn(ei), kOut(ei), 4*FiringRates(ei)+2);
plot([0 500], [0 500], 'k');
hold;

subplot(224); hold;
scatter(kInjc(~eijc), kOutjc(~eijc), 4*FiringRatesjc(~eijc)+2);
scatter(kInjc(eijc), kOutjc(eijc), 4*FiringRatesjc(eijc)+2);
plot([0 600], [0 600], 'k');

hold;