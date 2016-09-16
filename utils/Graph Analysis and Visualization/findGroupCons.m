% %Analyze1000Data;
analysisPop = 1E6;
numNull = 1000;
rewireInterval = analysisPop/numNull;
numNets = 45;
maxVal = 432;
minGroup = 3;
maxGroup = 6;
noGrps = maxGroup-minGroup+1;
noCons = cell(noGrps,1);
nullNoCons = cell(noGrps,1);
randGrps = cell(noGrps, 1);
for i = 1:noGrps
    disp(['Generating ' num2str(analysisPop) ' random node combinations for ' ...
        num2str(minGroup+i-1) '-groups...']);
    randGrps{i} = combSelect(maxVal, minGroup+i-1, analysisPop);
end
disp('Non-repeating combination generation complete');


disp('Begin tally for networks');
for i = 1:noGrps
    disp([num2str(minGroup+i-1) '-groups...']);
    noConsNets = zeros(numNets, (minGroup+i-1) * (minGroup+i-2) + 1);
    tbl = randGrps{i};
    parfor j = 1:numNets
        wt = exMats(:,:,j) ~= 0;
        noConsLoc = zeros(1, (minGroup+i-1) * (minGroup+i-2) + 1);
        for k = 1:analysisPop
            nc = nnz(wt(tbl(k,:), tbl(k,:)));
            noConsLoc(nc+1) = noConsLoc(nc+1) + 1;
        end
        noConsNets(j,:) = noConsLoc;
    end
    noCons{i} = noConsNets;
end

disp('Begin tally for null models');
for i = 1:noGrps
    disp([num2str(minGroup+i-1) '-groups (null)...']);
    noConsNets = zeros(numNets, (minGroup+i-1) * (minGroup+i-2) + 1);
    tbl = randGrps{i};
    parfor j = 1:numNets
        wt = dir_generate_srand_bid_prev(exMats(:,:,j) ~= 0);
        noConsLoc = zeros(1, (minGroup+i-1) * (minGroup+i-2) + 1);
        for k = 1:analysisPop
            if mod(k, rewireInterval) == 0
                wt = dir_generate_srand_bid_prev(exwts{j} ~= 0);
            end
            nc = nnz(wt(tbl(k,:), tbl(k,:)));
            noConsLoc(nc+1) = noConsLoc(nc+1) + 1;
        end
        noConsNets(j,:) = noConsLoc;
    end
    nullNoCons{i} = noConsNets;
end

stdUp = cell(4,1);
stdLw = cell(4,1);
stdUpN = cell(4,1);
stdLwN = cell(4,1);
ksVals = cell(4,1);

figure; hold;

for i = 1:noGrps
    subplot(1,4,i); hold on;
    noCons{i} = bsxfun(@rdivide, noCons{i}, sum(noCons{i},2));
    nullNoCons{i} = bsxfun(@rdivide, nullNoCons{i}, sum(nullNoCons{i},2));
    [stdLw{i}, stdUp{i}] = semistd(noCons{i});
    [stdLwN{i}, stdUpN{i}] = semistd(nullNoCons{i});
    errbar(0:(length(noCons{i}(1,:))-1), mean(noCons{i}), stdLw{i}, ...
        stdUp{i}, 'r-');
    plot(0:(length(noCons{i}(1,:))-1), mean(noCons{i}), 'r-');
    errbar(0:(length(noCons{i}(1,:))-1), mean(nullNoCons{i}), stdLwN{i}, ...
        stdUpN{i}, 'b-');
    plot(0:(length(noCons{i}(1,:))-1), mean(nullNoCons{i}), 'b-');
    set(gca, 'YScale', 'log');
    title([num2str(minGroup+i-1) '-clusters']);
    kv = zeros(1, length(noCons{i}(1,:)));
    for j = 1:length(noCons{i}(1,:))
        [~, kv(j)] = kstest2(noCons{i}(:,j), nullNoCons{i}(:,j));
    end
    ksVals{i} = kv;
    siginds = find(kv < 0.01);
    mv = mean(noCons{i});
    %      for j = 1:length(siginds)
    %         if kv(siginds(j))
    %         end
    %      scatter(siginds-.7, 2.5*mv(siginds), (-log10(kv(siginds))).^2*5, 'k*');
    hold off;
end

