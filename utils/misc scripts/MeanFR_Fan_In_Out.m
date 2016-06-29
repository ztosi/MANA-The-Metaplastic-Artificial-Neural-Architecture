figure; 
subplot(121); 
title('Mean Firing Rate of Fan-In');
hold on;
scatter(FiringRates(~ei), meanNZ(bsxfun(@times, wtMat(:, ~ei)~=0, ...
    FiringRates')), (kIn(~ei)+25)/2.5, repmat([0 0.45 0.74], sum(~ei), 1), ...
    'filled', 'MarkerFaceAlpha', 0.5);
scatter(FiringRates(ei), meanNZ(bsxfun(@times, wtMat(:, ei)~=0, ...
    FiringRates')), (kIn(ei)+25)/2.5, repmat([0.75 0.25 0.1], sum(ei), 1), ...
    'filled', 'MarkerFaceAlpha', 0.5);
hold off;
xlabel('Firing Rates');
ylabel('Mean Feed-In');
%zlabel('In Degree');
set(gca, 'XScale', 'log');%, 'ZScale', 'log');

subplot(122); 
title('Mean Firing Rate of Fan-Out');
hold on;
scatter(FiringRates(~ei), meanNZ(bsxfun(@times, wtMat(~ei,:)~=0, ...
    FiringRates),2), (kOut(~ei)+25)/2.5, repmat([0 0.45 0.74], sum(~ei), 1), ...
    'filled','MarkerFaceAlpha', 0.5);
scatter(FiringRates(ei), meanNZ(bsxfun(@times, wtMat(ei,:)~=0, ...
    FiringRates),2), (kOut(ei)+25)/2.5, repmat([0.75 0.25 0.1], sum(ei), 1), ...
    'filled', 'MarkerFaceAlpha', 0.5);
hold off;
xlabel('Firing Rates');
ylabel('Mean Outgoing');
%zlabel('Out Degree');
set(gca, 'XScale', 'log');%, 'ZScale', 'log');
