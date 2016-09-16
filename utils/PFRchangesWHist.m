f = figure;
set(f, 'Position', [50 50 1500 650]);
set(f, 'NextPlot', 'replace');
ylimU=5;
ylimD=-5;
%bins = ylimD:0.25:ylimU;

subplot(3, 3, [1 2 4 5 7 8]);
hold on;
xlabel('Time (s)');
ylabel('Log(FiringRates (Hz))');
ylim([ylimD ylimU]);
xlim([1 t]);
plot(repmat([1:t]', 1, sum(ei)),log(PFs(1:t, ei)), 'Color',...
    [0.85 0.33 0.1]);
plot(repmat([1:t]', 1, sum(~ei)), log(PFs(1:t, ~ei)), 'Color', ...
    [0 0.43 0.75]);

l1 = 50;
l2 = 650;
l3 = 2500;

% ...
%'Color', [0 0.43 0.75]);
plot([l1 l1], [ylimD ylimU], 'LineWidth', 3);
plot([l2 l2], [ylimD ylimU], 'LineWidth', 3);
plot([l3 l3], [ylimD ylimU], 'LineWidth', 3);
hold off;

subplot(333);
%hold on;
%h=overlayedHistogram(log(PFs(i,~ei)), log(PFs(i,ei)), 20, 'Normalization', 'probability');
h=histogram(log(PFs(l1,:)),'BinMethod', 'fd',  'Normalization', 'pdf' );
%[bh, be] = histcounts(log(PFs(i,:)), bins, 'Normalization', 'probability' );
%sz = be(2)-be(1);
%p=patch([be(1)-sz be(1:(end-1))+(sz/2) be(end)+sz], [0 bh 0 ], [0 0.43 0.75]);
h.EdgeColor = 'none';
h.FaceAlpha = 0.7;
xlabel('Log(FiringRates (Hz))');
%ylabel('Counts');
%xlim([ylimD ylimU]);
%ylim([0 .15]);
%view(90,-90);
title(['Time: ' num2str(l1) 's']);
subplot(336);
h=histogram(log(PFs(l2,:)),'BinMethod', 'fd',  'Normalization', 'pdf' );
%[bh, be] = histcounts(log(PFs(i,:)), bins, 'Normalization', 'probability' );
%sz = be(2)-be(1);
%p=patch([be(1)-sz be(1:(end-1))+(sz/2) be(end)+sz], [0 bh 0 ], [0 0.43 0.75]);
h.EdgeColor = 'none';
h.FaceAlpha = 0.7;
%for k=1:length(h)
%    h(k).EdgeColor = 'none';
%end
xlabel('Log(FiringRates (Hz))');
%ylabel('Counts');
%xlim([ylimD ylimU]);
%ylim([0 .15]);
%view(90,-90);
title(['Time: ' num2str(l2) 's']);
subplot(339);
h=histogram(log(PFs(l3,:)), 'BinMethod', 'fd', 'Normalization', 'pdf' );
%[bh, be] = histcounts(log(PFs(i,:)), bins, 'Normalization', 'probability' );
%sz = be(2)-be(1);
%p=patch([be(1)-sz be(1:(end-1))+(sz/2) be(end)+sz], [0 bh 0 ], [0 0.43 0.75]);
h.EdgeColor = 'none';
h.FaceAlpha = 0.7;
xlabel('Log(FiringRates (Hz))');
%ylabel('Counts');
%xlim([ylimD ylimU]);
%ylim([0 .15]);
%view(90,-90);
title(['Time: ' num2str(l3) 's']);


