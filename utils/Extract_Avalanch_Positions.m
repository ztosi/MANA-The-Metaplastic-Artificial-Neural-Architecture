N = length(PrefFRs);
posCount = zeros(N, max(len));
avgRichness = zeros(1, length(len));
meanDuration = zeros(N, 1);
avgRichness_len = zeros(max(unique(len)), 1);
for i = 1:length(str_aval)
   tmp = str_aval{i};
   tmp2 = zeros(1, length(tmp));
   
   %if length(tmp) > 50
    meanDuration(tmp(:, 1)) = meanDuration(tmp(:, 1)) + length(tmp);
    for j = 1:length(tmp)
        posCount(tmp(j, 1), tmp(j,2)) = posCount(tmp(j, 1), ...
            tmp(j,2)) + 1;
        tmp2(j) = richness(tmp(j, 1));
    end
    avgRichness_len( max(tmp(:, 2)) ) = avgRichness_len(max(tmp(:, 2))) ...
        + (sum(richness(tmp(:, 1))));
    avgRichness(i) = mean(tmp2);
   %end
end
avgRichness_len = avgRichness_len' ./ (1:max(unique(len)));