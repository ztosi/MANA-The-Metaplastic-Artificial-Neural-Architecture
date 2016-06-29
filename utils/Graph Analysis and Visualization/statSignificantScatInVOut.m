NUM_NULL = 50;

[m, n] = size(wts);

nullMOI = zeros(NUM_NULL, n);
nullSOI = zeros(NUM_NULL, n);

[inDeg, nullMOI(1, :), nullSOI(1, :)] = scatterInDegVsOutOfIns( ...
    dir_generate_srand(wts), 0);

for i = 2:NUM_NULL  
   [inDeg, nullMOI(i, :), nullSOI(i, :)] = scatterInDegVsOutOfIns( ...
    dir_generate_srand(wts), 0);
end

finalNullMoiMean = mean(nullMOI);
finalNullMoiStd = std(nullMOI);
finalNullSoiMean = mean(nullSOI);
finalNullSoiStd = std(nullSOI);

figure; hold;
scatter(inDeg, finalNullMoiMean, 'bo');
[~, avgOIM, stdOIM] = scatterInDegVsOutOfIns(wts, 0);
title('Means');
for i = 1:n
    plot([inDeg(i), inDeg(i)], [finalNullMoiMean(i) - finalNullMoiStd(i), ...
        finalNullMoiMean(i) + finalNullMoiStd(i)]);
end
a = 25;
colormap hot;
c = abs(avgOIM - finalNullMOI) ./ finalNullMoiStd;
scatter(inDeg, avgOIM, a, c, 'filled');
hold;


figure; hold;
scatter(inDeg, finalNullSoiMean, 'bo');
title('Std. Devs');
for i = 1:n
    plot([inDeg(i), inDeg(i)], [finalNullSoiMean(i) - finalNullSoiStd(i), ...
        finalNullSoiMean(i) + finalNullSoiStd(i)]);
end
a = 25;
colormap hot;
c = abs(stdOIM - finalNullSOI) ./ finalNullSoiStd;
scatter(inDeg, avgOIM, a, c, 'filled');
scatter(inDeg, stdOIM, 'r.');
hold;
