f = figure;
set(f, 'Position', [50 50 1500 650]);
set(f, 'NextPlot', 'replace');
ylimU=5;
ylimD=-3;
bins = ylimD:0.25:ylimU;
interval = 5;
for i=50:interval:t
    clf;
    subplot(121);
    hold on;
    xlabel('Time (s)');
    ylabel('Log(FiringRates (Hz))');
    ylim([ylimD ylimU]);
    xlim([1 t]);
    for j=1:length(FiringRates)
        if ei(j) == 1
            patchline([2:i],log(PFs(2:i, j))', 'edgecolor',...
                [0.85 0.33 0.1], 'edgealpha', 0.25);
        else
            patchline([2:i], log(PFs(2:i, j))', 'edgecolor', ...
                [0 0.43 0.75], 'edgealpha', 0.25);
        end
    end


    % ...
        %'Color', [0 0.43 0.75]);
    plot([i i], [ylimD ylimU], 'k', 'LineWidth', 3);
    hold off;
    
    subplot(122);
    ylim([ylimD ylimU]);
    %hold on;
    %h=overlayedHistogram(log(PFs(i,~ei)), log(PFs(i,ei)), 20, 'Normalization', 'probability');
    h=histogram(log(PFs(i,:)), bins, 'Normalization', 'probability' );
    %[bh, be] = histcounts(log(PFs(i,:)), bins, 'Normalization', 'probability' );
    %sz = be(2)-be(1);
    %p=patch([be(1)-sz be(1:(end-1))+(sz/2) be(end)+sz], [0 bh 0 ], [0 0.43 0.75]);
    h.EdgeColor = 'none';
    h.FaceAlpha = 0.7;
    %for k=1:length(h)
    %    h(k).EdgeColor = 'none';
    %end
    
    %h.FaceAlpha = 0.75;
    xlabel('Log(FiringRates (Hz))');
    ylabel('Counts');
    xlim([ylimD ylimU]);
    ylim([0 .15]);
    view(90,-90);
    title(['Time: ' num2str(i-2) 's; ' num2str(sum(flipped)/length(flipped)) '%']);
    %hold off;
    
    drawnow;
    
    frame = getframe(f);
    im = frame2im(frame);
    [imind, cm] = rgb2ind(im, 256);
    
    if i==2
        imwrite(imind,cm,'~/FiringRateEvolution.gif','gif', 'DelayTime',0, ...
            'Loopcount', inf);
    else
        imwrite(imind,cm,'~/FiringRateEvolution.gif','gif', 'DelayTime', 0, ...
            'WriteMode', 'append');
    end
    
    
end